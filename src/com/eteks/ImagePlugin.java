package com.eteks;

import com.eteks.sweethome3d.j3d.PhotoRenderer;
import com.eteks.sweethome3d.j3d.AbstractPhotoRenderer.Quality;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeLight;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.HomeFurnitureGroup;
import com.eteks.sweethome3d.plugin.Plugin;
import com.eteks.sweethome3d.plugin.PluginAction;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.JButton;

public class ImagePlugin extends Plugin {

    public PluginAction[] getActions() {
        return new PluginAction[] { new ImagePlugin.HomeAssistantLightningAction() };
    }

    void recursivelyCombine(List<List<Float>> result, List<Float> current, List<Float> in1, List<Float> in2, int index) {
        if (index == in1.size()) {
            result.add(current);
        } else if (((Float) in1.get(index)).equals(in2.get(index))) {
            current.add((Float) in1.get(index));
            this.recursivelyCombine(result, current, in1, in2, index + 1);
        } else {
            List<Float> temp = new ArrayList<Float>(current);
            temp.add(in1.get(index));
            this.recursivelyCombine(result, temp, in1, in2, index + 1);
            temp = new ArrayList<Float>(current);
            temp.add(in2.get(index));
            this.recursivelyCombine(result, temp, in1, in2, index + 1);
        }
    }

    public class HomeAssistantLightningAction extends PluginAction {
        private Home home;

        public void execute() {
            this.home = ImagePlugin.this.getHome();
            HomeAssistantLightningOptions options = this.createInputPanel();
            if (options != null) {
                List<HomePieceOfFurniture> lights = new ArrayList<>();
                this.getAllHomeLights(this.home.getFurniture(), lights);
                if (this.createInfoPanel(lights)) {
                    try {
                        Map<HomePieceOfFurniture, Float> initialPowerValues = new HashMap<>();
                        Map<HomePieceOfFurniture, Boolean> initialVisibilityValues = new HashMap<>();
                        for (HomePieceOfFurniture light : lights) {
                            if (light instanceof HomeLight) {
                                initialPowerValues.put(light, ((HomeLight) light).getPower());
                                initialVisibilityValues.put(light, light.isVisible());
                                light.setVisible(true); // Ensure all lights are visible
                            }
                        }
                        List<List<Float>> combinations = this.generateYaml(options.getPath(), options.getHaPath(), lights, initialPowerValues);
                        JOptionPane.showInternalMessageDialog(null, "YAML generated successfully!");
                        this.generateImagesWithProgress(options, combinations, lights, initialPowerValues, initialVisibilityValues);
                    } catch (IOException var4) {
                        var4.printStackTrace();
                        JOptionPane.showInternalMessageDialog(null, "Error: " + var4.getMessage());
                    }
                }
            }
        }

        public boolean createInfoPanel(List<HomePieceOfFurniture> lights) {
            int lightCount = lights.size();
            double imageCount = Math.pow(2.0D, (double) lightCount);
            String message = String.format("I've found %d lights, so I'll generate %.0f images. Is that OK?", lightCount, imageCount);

            return JOptionPane.showConfirmDialog(null, message, "Information", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        }

        public HomeAssistantLightningOptions createInputPanel() {
            JTextField pathField = new JTextField(5);
            JTextField widthField = new JTextField(5);
            JTextField heightField = new JTextField(5);
            JTextField qualityField = new JTextField(5);
            JTextField haPathField = new JTextField(5);
            JPanel inputPanel = new JPanel();
            inputPanel.add(new JLabel("Output path:"));
            inputPanel.add(pathField);
            inputPanel.add(Box.createVerticalStrut(15));
            inputPanel.add(new JLabel("Image width:"));
            inputPanel.add(widthField);
            inputPanel.add(Box.createVerticalStrut(15));
            inputPanel.add(new JLabel("Image height:"));
            inputPanel.add(heightField);
            inputPanel.add(Box.createVerticalStrut(15));
            inputPanel.add(new JLabel("Quality (high or low):"));
            inputPanel.add(qualityField);
            inputPanel.add(Box.createVerticalStrut(15));
            inputPanel.add(new JLabel("Home Assistant path:"));
            inputPanel.add(haPathField);
            int result = JOptionPane.showConfirmDialog(null, inputPanel, "Please fill the values", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                return new HomeAssistantLightningOptions(pathField.getText(), Integer.parseInt(widthField.getText()),
                        Integer.parseInt(heightField.getText()),
                        qualityField.getText().equalsIgnoreCase("high") ? Quality.HIGH : Quality.LOW,
                        haPathField.getText());
            } else {
                return null;
            }
        }

        public HomeAssistantLightningAction() {
            this.putPropertyValue(Property.NAME, "Home Assistant Lightning Export");
            this.putPropertyValue(Property.MENU, "Tools");
            this.setEnabled(true);
        }

        private List<List<Float>> generateYaml(String path, String haPath, List<HomePieceOfFurniture> lights, Map<HomePieceOfFurniture, Float> initialPowerValues) throws IOException {
            List<List<Float>> results = new ArrayList<List<Float>>();
            List<Float> in1 = new ArrayList<Float>();
            List<Float> in2 = new ArrayList<Float>();

            for (HomePieceOfFurniture light : lights) {
                in1.add(initialPowerValues.get(light));
                in2.add(0.0F);
            }

            ImagePlugin.this.recursivelyCombine(results, new ArrayList<Float>(), in1, in2, 0);
            String yaml = "type: picture-elements\nimage: " + haPath + "/casa_noche.jpg\nelements:\n";

            for (List<Float> list : results) {
                StringBuilder imageNameBuilder = new StringBuilder();
                yaml = yaml + "  - conditions:\r\n";

                int ix;
                for (ix = 0; ix < list.size(); ++ix) {
                    String state = (double) (Float) list.get(ix) == 0.0D ? "off" : "on";
                    imageNameBuilder.append(state);
                    yaml = yaml + "      - entity: " + ((HomePieceOfFurniture) lights.get(ix)).getName() + "\r\n"
                            + "        state: '" + state + "'\r\n";
                }

                String imageName = imageNameBuilder.toString();
                yaml = yaml + "    elements:\r\n      - entity:\r\n";

                for (ix = 0; ix < list.size(); ++ix) {
                    yaml = yaml + "          - " + ((HomePieceOfFurniture) lights.get(ix)).getName() + "\r\n";
                }

                yaml = yaml + "        filter: brightness(100%)\r\n        image: " + haPath + "/" + imageName + ".jpg\r\n"
                        + "        style:\r\n" + "          left: 50%\r\n" + "          top: 50%\r\n"
                        + "          width: 100%\r\n" + "        type: image\r\n" + "    type: conditional\r\n";

                // Check if all states are off and update the base image
                if (!imageName.contains("on")) {
                    yaml = yaml.replace("image: " + haPath + "/casa_noche.jpg", "image: " + haPath + "/" + imageName + ".jpg");
                }
            }

            FileWriter outputfile = new FileWriter(path + "/config.yaml");
            outputfile.write(yaml);
            outputfile.close();
            return results;
        }

        private void generateImagesWithProgress(HomeAssistantLightningOptions options, List<List<Float>> combinations, List<HomePieceOfFurniture> lights, Map<HomePieceOfFurniture, Float> initialPowerValues, Map<HomePieceOfFurniture, Boolean> initialVisibilityValues) {
            final int totalImages = combinations.size();
            final JProgressBar progressBar = new JProgressBar(0, totalImages);
            final JLabel progressLabel = new JLabel("Images saved: 0 / " + totalImages + " (0%)");
            final JButton cancelButton = new JButton("Cancel");

            JPanel panel = new JPanel();
            panel.add(progressLabel);
            panel.add(progressBar);
            panel.add(cancelButton);

            final JDialog dialog = new JDialog();
            dialog.setTitle("Progress");
            dialog.setModal(false); // Make the dialog non-modal
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.getContentPane().add(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    generateImages(options, combinations, lights, progressBar, progressLabel, totalImages, initialPowerValues, initialVisibilityValues, this);
                    return null;
                }

                @Override
                protected void done() {
                    dialog.dispose();
                    JOptionPane.showMessageDialog(null, "Image generation completed!");
                }
            };
            
            cancelButton.addActionListener(e -> {
                worker.cancel(true);
                dialog.dispose();
                JOptionPane.showMessageDialog(null, "Image generation cancelled!");
            });            

            worker.execute();
        }

        private void generateImages(HomeAssistantLightningOptions options, List<List<Float>> combinations, List<HomePieceOfFurniture> lights, JProgressBar progressBar, JLabel progressLabel, int totalImages, Map<HomePieceOfFurniture, Float> initialPowerValues, Map<HomePieceOfFurniture, Boolean> initialVisibilityValues, SwingWorker<Void, Void> worker) {
            Iterator<List<Float>> var5 = combinations.iterator();

            // Print the initial list of all lights with their numbers and power values
            System.out.println("List of all lights:");
            for (int i = 0; i < lights.size(); i++) {
                HomePieceOfFurniture light = lights.get(i);
                System.out.println((i + 1) + ": " + light.getName() + " - Power: " + initialPowerValues.get(light));
            }
            System.out.println("-----------------------------------");

            try {
                int combinationIndex = 0;
                while (var5.hasNext()) {
                    combinationIndex++;
                    List<Float> list = (List<Float>) var5.next();
                    StringBuilder imageNameBuilder = new StringBuilder();
                    ArrayList<HomeLight> currentLights = new ArrayList<HomeLight>();

                    for (int l = 0; l < list.size(); l++) {
                        if (lights.size() > l) {
                            HomeLight light = (HomeLight) lights.get(l);

                            // Log the light details before setting power
                            System.out.println("Setting power for: " + light.getName() + " (Combination: " + combinationIndex + ", Light Number: " + (l + 1) + ")");
                            System.out.println("Original Power: " + light.getPower() + ", New Power: " + list.get(l));
                            
                            light.setPower(list.get(l));
                            currentLights.add(light);

                            // Append state to image name
                            imageNameBuilder.append((double) list.get(l) == 0.0D ? "off" : "on");

                            // Print the details of each light with its number
                            System.out.println("Combination " + combinationIndex + " - Light Number: " + (l + 1));
                            System.out.println("Light Name: " + light.getName());
                            System.out.println("Power: " + light.getPower());
                        }
                    }

                    String imageName = imageNameBuilder.toString();
                    System.out.println("Image Name: " + imageName);

                    System.out.println("-----------------------------------");
                    ArrayList<HomePieceOfFurniture> itemsToRemove = new ArrayList<HomePieceOfFurniture>();

                    int i;
                    for (i = 0; i < this.home.getFurniture().size(); ++i) {
                        if (this.home.getFurniture().get(i) instanceof HomeLight) {
                            itemsToRemove.add((HomePieceOfFurniture) this.home.getFurniture().get(i));
                        }
                    }

                    for (i = 0; i < currentLights.size(); ++i) {
                        this.home.addPieceOfFurniture((HomePieceOfFurniture) currentLights.get(i));
                    }

                    for (i = 0; i < itemsToRemove.size(); ++i) {
                        this.home.deletePieceOfFurniture((HomePieceOfFurniture) itemsToRemove.get(i));
                    }

                    this.createImage(this.home, options, imageName);

                    // Revert the power values to the initial values
                    for (HomeLight light : currentLights) {
                        light.setPower(initialPowerValues.get(light));
                    }

                    // Update progress bar and label
                    final int progress = combinationIndex;
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress);
                        progressLabel.setText("Images saved: " + progress + " / " + totalImages + " (" + (progress * 100 / totalImages) + "%)");
                    });
                    
                    if (Thread.currentThread().isInterrupted() || worker.isCancelled()) {
                        throw new InterruptedException("Image generation cancelled");
                    }                    
                }

                // Restore the initial visibility state
                for (Map.Entry<HomePieceOfFurniture, Boolean> entry : initialVisibilityValues.entrySet()) {
                    entry.getKey().setVisible(entry.getValue());
                }

            } catch (Exception e) {
                JOptionPane.showInternalMessageDialog(null, "Error: " + e.getMessage());
            }
        }

        private void createImage(Home home, HomeAssistantLightningOptions options, String name) {
            try {
                System.out.println("Generating image " + options.getPath() + "/" + name);
                long millis = System.currentTimeMillis();
                PhotoRenderer renderer = new PhotoRenderer(home, options.getQuality());
                BufferedImage image = new BufferedImage(options.getImageWidth(), options.getImageHeight(), BufferedImage.TYPE_INT_RGB);
                renderer.render(image, home.getCamera(), new ImageObserver() {
                    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
                        if ((infoflags & ImageObserver.ALLBITS) != 0) {
                            // A imagem está completamente carregada
                            System.out.println("Image loaded!");
                            return true;
                        }
                        return false; 
                    }
                });

                File outputfile = new File(options.getPath() + "/" + name + ".jpg");
                ImageIO.write(image, "jpg", outputfile);
                System.out.println("Image generated, Time needed:" + (System.currentTimeMillis() - millis));
            } catch (IOException var9) {
                JOptionPane.showMessageDialog(null, "Error: " + var9.getMessage());
                var9.printStackTrace();
            }
        }

        public void getAllHomeLights(List<HomePieceOfFurniture> furnitureList, List<HomePieceOfFurniture> lights) {
            for (HomePieceOfFurniture piece : furnitureList) {
                if (piece instanceof HomeLight) {
                    lights.add(piece);
                } else if (piece instanceof HomeFurnitureGroup) {
                    getAllHomeLights(((HomeFurnitureGroup) piece).getFurniture(), lights);
                }
            }
        }
    }
}
