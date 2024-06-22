package com.eteks;

import com.eteks.sweethome3d.j3d.PhotoRenderer;
import com.eteks.sweethome3d.j3d.AbstractPhotoRenderer.Quality;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeLight;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.HomeFurnitureGroup;
import com.eteks.sweethome3d.plugin.Plugin;
import com.eteks.sweethome3d.plugin.PluginAction;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.*;
import java.util.*;
import java.util.List;
import java.text.NumberFormat;

public class ImagePlugin extends Plugin {

    // Return plugin actions
    public PluginAction[] getActions() {
        return new PluginAction[] { new ImagePlugin.HomeAssistantLightningAction() };
    }

    // Recursively generate all combinations of lights and groups being on or off
    void recursivelyCombine(List<List<Float>> result, List<Float> current, List<Float> in1, List<Float> in2, int index, List<Boolean> isGroup) {
        // If index equals the size of in1, add current combination to result
        if (index == in1.size()) {
            result.add(new ArrayList<>(current));
        // If the elements at the current index in in1 and in2 are the same, add to current and proceed
        } else if (in1.get(index).equals(in2.get(index))) {
            current.add(in1.get(index));
            this.recursivelyCombine(result, current, in1, in2, index + 1, isGroup);
        // Otherwise, branch into two paths: one with the element from in1 and one with the element from in2
        } else {
            List<Float> temp = new ArrayList<>(current);
            temp.add(in1.get(index));
            this.recursivelyCombine(result, temp, in1, in2, index + 1, isGroup);
            temp = new ArrayList<>(current);
            temp.add(in2.get(index));
            this.recursivelyCombine(result, temp, in1, in2, index + 1, isGroup);
        }
    }

    // Plugin action to export lighting data to Home Assistant
    public class HomeAssistantLightningAction extends PluginAction {
        private Home home;

        // Execute the action
        public void execute() {

            // Get the current home project
            this.home = ImagePlugin.this.getHome();

            // Check if no project is open
            if (this.home == null || this.home.getFurniture().isEmpty()) {
                // Show an error message if no project is open
                JOptionPane.showMessageDialog(null, "No project is open. Please open a project before proceeding.", "Error", JOptionPane.ERROR_MESSAGE);
                return; // Exit the method to cancel the process
            }
            
            // Check if the project has no lights
            List<HomePieceOfFurniture> allLights = new ArrayList<>();
            List<HomeFurnitureGroup> groups = new ArrayList<>();
            this.getAllHomeLightsAndGroups(this.home.getFurniture(), allLights, groups);

            // Show an error message if the project has no lights
            if (allLights.isEmpty()) {
                JOptionPane.showMessageDialog(null, "The project has no lights. Please add lights to the project before proceeding.", "Error", JOptionPane.ERROR_MESSAGE);
                return; // Exit the method to cancel the process
            }

            // Create the input panel for user options
            HomeAssistantLightningOptions options = this.createInputPanel();
            if (options != null) {
                List<HomePieceOfFurniture> lightsNotInGroups = new ArrayList<>();

                // Get the lights that are not in any groups
                this.getLightsNotInGroups(allLights, groups, lightsNotInGroups);

                // If the user confirms the info panel, proceed with generating YAML and images
                if (this.createInfoPanel(lightsNotInGroups, groups)) {
                    try {
                        Map<HomePieceOfFurniture, Float> initialPowerValues = new HashMap<>();
                        Map<HomePieceOfFurniture, Boolean> initialVisibilityValues = new HashMap<>();
                        // Store initial power and visibility values of lights
                        for (HomePieceOfFurniture light : allLights) {
                            if (light instanceof HomeLight) {
                                initialPowerValues.put(light, ((HomeLight) light).getPower());
                                initialVisibilityValues.put(light, light.isVisible());
                                light.setVisible(true); // Ensure all lights are visible
                            }
                        }
                        // Ensure lights in groups are also in the initialPowerValues map
                        for (HomeFurnitureGroup group : groups) {
                            for (HomePieceOfFurniture light : group.getFurniture()) {
                                if (light instanceof HomeLight) {
                                    initialPowerValues.putIfAbsent(light, ((HomeLight) light).getPower());
                                }
                            }
                        }

                        // Generate YAML combinations
                        List<List<Float>> combinations = this.generateYaml(options.getPath(), options.getHaPath(), lightsNotInGroups, groups, initialPowerValues);
                        JOptionPane.showInternalMessageDialog(null, "YAML generated successfully!");
                        // Generate images with progress tracking
                        this.generateImagesWithProgress(options, combinations, lightsNotInGroups, groups, initialPowerValues, initialVisibilityValues);
                    } catch (IOException var4) {
                        var4.printStackTrace();
                        JOptionPane.showInternalMessageDialog(null, "Error: " + var4.getMessage());
                    }
                }

                // Set up the log file
                File logFile = new File(options.getPath() + "/log.txt");
                try {
                    PrintStream logStream = new PrintStream(new FileOutputStream(logFile));
                    System.setOut(logStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error setting up log file: " + e.getMessage());
                    return;
                }
                
            }
        }

        public boolean createInfoPanel(List<HomePieceOfFurniture> lightsNotInGroups, List<HomeFurnitureGroup> groups) {
            int lightCount = lightsNotInGroups.size();
            int groupCount = groups.size();
            int totalItems = lightCount + groupCount;
            double imageCount = Math.pow(2.0D, (double) totalItems);

            // Format the image count with comma separator
            NumberFormat numberFormat = NumberFormat.getInstance();
            String formattedImageCount = numberFormat.format(imageCount);

            String message = String.format("I've found %d individual lights and %d groups, so I'll generate %s images. Total entities: %d. Is that OK?", lightCount, groupCount, formattedImageCount, totalItems);

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.BOTH;

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            panel.add(new JLabel(message), gbc);

            DefaultListModel<String> lightsListModel = new DefaultListModel<>();
            for (HomePieceOfFurniture light : lightsNotInGroups) {
                lightsListModel.addElement(light.getName());
            }
            JList<String> lightsList = new JList<>(lightsListModel);
            JScrollPane lightsScrollPane = new JScrollPane(lightsList);
            lightsScrollPane.setPreferredSize(new Dimension(150, 200));

            DefaultListModel<String> groupsListModel = new DefaultListModel<>();
            for (HomeFurnitureGroup group : groups) {
                groupsListModel.addElement(group.getName());
            }
            JList<String> groupsList = new JList<>(groupsListModel);
            JScrollPane groupsScrollPane = new JScrollPane(groupsList);
            groupsScrollPane.setPreferredSize(new Dimension(150, 200));

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 1;
            panel.add(new JLabel("Lights list with no group"), gbc);

            gbc.gridx = 1;
            panel.add(new JLabel("Group list with lights in"), gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0.5;
            gbc.weighty = 1.0;
            panel.add(lightsScrollPane, gbc);

            gbc.gridx = 1;
            panel.add(groupsScrollPane, gbc);

            return JOptionPane.showConfirmDialog(null, panel, "Information", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        }

        // Create an input panel for the user to specify paths, image dimensions, and quality
        public HomeAssistantLightningOptions createInputPanel() {
            JTextField pathField = new JTextField(20);
            JTextField haPathField = new JTextField(20);
            JTextField widthField = new JTextField(5);
            JTextField heightField = new JTextField(5);
            JComboBox<String> qualityComboBox = new JComboBox<>(new String[]{"high", "low"});

            JPanel inputPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.EAST;
            inputPanel.add(new JLabel("Output path:"), gbc);
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            inputPanel.add(pathField, gbc);

            gbc.gridx = 2;
            gbc.anchor = GridBagConstraints.EAST;
            inputPanel.add(new JLabel("Home Assistant path:"), gbc);
            gbc.gridx = 3;
            gbc.anchor = GridBagConstraints.WEST;
            inputPanel.add(haPathField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.anchor = GridBagConstraints.EAST;
            inputPanel.add(new JLabel("Image width:"), gbc);
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            inputPanel.add(widthField, gbc);

            gbc.gridx = 2;
            gbc.anchor = GridBagConstraints.EAST;
            inputPanel.add(new JLabel("Image height:"), gbc);
            gbc.gridx = 3;
            gbc.anchor = GridBagConstraints.WEST;
            inputPanel.add(heightField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.anchor = GridBagConstraints.EAST;
            inputPanel.add(new JLabel("Quality:"), gbc);
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            inputPanel.add(qualityComboBox, gbc);

            int result = JOptionPane.showConfirmDialog(null, inputPanel, "Please fill the values", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                return new HomeAssistantLightningOptions(pathField.getText(), 
                                                         Integer.parseInt(widthField.getText()), 
                                                         Integer.parseInt(heightField.getText()), 
                                                         qualityComboBox.getSelectedItem().equals("high") ? Quality.HIGH : Quality.LOW, 
                                                         haPathField.getText());
            } else {
                return null;
            }
        }

        // Constructor to set up the plugin action properties
        public HomeAssistantLightningAction() {
            this.putPropertyValue(Property.NAME, "Home Assistant Lightning Export");
            this.putPropertyValue(Property.MENU, "Tools");
            this.setEnabled(true);
        }

        // Generate YAML file with all possible combinations of light states
        private List<List<Float>> generateYaml(String path, String haPath, List<HomePieceOfFurniture> lightsNotInGroups, List<HomeFurnitureGroup> groups, Map<HomePieceOfFurniture, Float> initialPowerValues) throws IOException {
            System.out.println("Starting generateYaml method");
            System.out.println("-----------------------------------");
            List<List<Float>> results = new ArrayList<>();
            List<Float> in1 = new ArrayList<>();
            List<Float> in2 = new ArrayList<>();
            List<Boolean> isGroup = new ArrayList<>(); // Track if an item is part of a group

            // Add individual lights to in1 and in2 lists
            for (HomePieceOfFurniture light : lightsNotInGroups) {
                System.out.println("Adding light " + light.getName() + " with initial power " + initialPowerValues.get(light));
                in1.add(initialPowerValues.get(light)); // On
                in2.add(0.0F); // Off
                isGroup.add(false); // Individual light
            }

            // Add groups to in1 and in2 lists
            for (HomeFurnitureGroup group : groups) {
                System.out.println("Adding group " + group.getName());
                in1.add(1.0F); // Group On (indicator, not actual power)
                in2.add(0.0F); // Group Off
                isGroup.add(true); // Part of a group
            }

            // Generate all combinations of light states
            ImagePlugin.this.recursivelyCombine(results, new ArrayList<>(), in1, in2, 0, isGroup);
            System.out.println("Combinations generated: " + results.size());
            String yaml = "type: picture-elements\nimage: " + haPath + "/my_image.jpg\nelements:\n";

            // Create YAML entries for each combination
            for (List<Float> list : results) {
                StringBuilder imageNameBuilder = new StringBuilder();
                yaml = yaml + "  - conditions:\r\n";

                int ix = 0;
                for (HomePieceOfFurniture light : lightsNotInGroups) {
                    String state = (double) list.get(ix) == 0.0D ? "off" : "on";
                    imageNameBuilder.append(state);
                    yaml = yaml + "      - entity: " + light.getName() + "\r\n"
                            + "        state: '" + state + "'\r\n";
                    ix++;
                }
                for (HomeFurnitureGroup group : groups) {
                    String groupState = (double) list.get(ix) == 0.0D ? "off" : "on";
                    imageNameBuilder.append(groupState);
                    for (HomePieceOfFurniture light : group.getFurniture()) {
                        if (light instanceof HomeLight) {
                            yaml = yaml + "      - entity: " + light.getName() + "\r\n"
                                    + "        state: '" + groupState + "'\r\n";
                        }
                    }
                    ix++;
                }

                String imageName = imageNameBuilder.toString();
                yaml = yaml + "    elements:\r\n      - entity:\r\n";

                ix = 0;
                for (HomePieceOfFurniture light : lightsNotInGroups) {
                    yaml = yaml + "          - " + light.getName() + "\r\n";
                    ix++;
                }
                for (HomeFurnitureGroup group : groups) {
                    for (HomePieceOfFurniture light : group.getFurniture()) {
                        yaml = yaml + "          - " + light.getName() + "\r\n";
                        ix++;
                    }
                }

                yaml = yaml + "        filter: brightness(100%)\r\n        image: " + haPath + "/" + imageName + ".jpg\r\n"
                        + "        style:\r\n" + "          left: 50%\r\n" + "          top: 50%\r\n"
                        + "          width: 100%\r\n" + "        type: image\r\n" + "    type: conditional\r\n";

                // Check if all states are off and update the base image
                if (!imageName.contains("on")) {
                    yaml = yaml.replace("image: " + haPath + "/my_image.jpg", "image: " + haPath + "/" + imageName + ".jpg");
                }
            }

            // Write the YAML to a file
            FileWriter outputfile = new FileWriter(path + "/config.yaml");
            outputfile.write(yaml);
            outputfile.close();
            System.out.println("-----------------------------------");
            System.out.println("YAML generation completed");
            return results;
        }

        // Generate images with progress tracking
        private void generateImagesWithProgress(HomeAssistantLightningOptions options, List<List<Float>> combinations, List<HomePieceOfFurniture> lightsNotInGroups, List<HomeFurnitureGroup> groups, Map<HomePieceOfFurniture, Float> initialPowerValues, Map<HomePieceOfFurniture, Boolean> initialVisibilityValues) {
            System.out.println("Starting generateImagesWithProgress method");

            // Calculate the total number of images to be generated
            int totalImages = combinations.size(); // This should now be accurate
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

            // Capture start time
            long startTime = System.currentTimeMillis();

            // SwingWorker to generate images in the background
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    generateImages(options, combinations, lightsNotInGroups, groups, progressBar, progressLabel, totalImages, initialPowerValues, initialVisibilityValues, this);
                    return null;
                }

                @Override
                protected void done() {
                    dialog.dispose();
                    // Calculate and print elapsed time
                    long endTime = System.currentTimeMillis();
                    long elapsedTime = endTime - startTime;
                    System.out.println("Image generation completed!");
                    String formattedTime = formatElapsedTime(elapsedTime);
                    System.out.println("Total elapsed time: " + formattedTime);
                    JOptionPane.showMessageDialog(null, "Image generation completed!\nTotal elapsed time: " + formattedTime);
                }
            };

            cancelButton.addActionListener(e -> {
                worker.cancel(true);
                dialog.dispose();
                // Calculate and print elapsed time if cancelled
                long endTime = System.currentTimeMillis();
                long elapsedTime = endTime - startTime;
                System.out.println("Image generation cancelled!");
                String formattedTime = formatElapsedTime(elapsedTime);
                System.out.println("Total elapsed time: " + formattedTime);
                JOptionPane.showMessageDialog(null, "Image generation cancelled!\nTotal elapsed time: " + formattedTime);
            });

            worker.execute();
            System.out.println("SwingWorker execution started");
        }

        // Format elapsed time in milliseconds to HH:MM:SS
        private String formatElapsedTime(long milliseconds) {
            long seconds = (milliseconds / 1000) % 60;
            long minutes = (milliseconds / (1000 * 60)) % 60;
            long hours = (milliseconds / (1000 * 60 * 60)) % 24;
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }

        // Generate images for each combination of light states
        private void generateImages(HomeAssistantLightningOptions options, List<List<Float>> combinations, List<HomePieceOfFurniture> lightsNotInGroups, List<HomeFurnitureGroup> groups, JProgressBar progressBar, JLabel progressLabel, int totalImages, Map<HomePieceOfFurniture, Float> initialPowerValues, Map<HomePieceOfFurniture, Boolean> initialVisibilityValues, SwingWorker<Void, Void> worker) {
            System.out.println("Starting generateImages method");
            System.out.println("-----------------------------------");
            Iterator<List<Float>> var5 = combinations.iterator();

            try {
                int combinationIndex = 0;
                while (var5.hasNext()) {
                    combinationIndex++;
                    List<Float> list = var5.next();
                    StringBuilder imageNameBuilder = new StringBuilder();
                    ArrayList<HomeLight> currentLights = new ArrayList<>();

                    int ix = 0;
                    for (HomePieceOfFurniture light : lightsNotInGroups) {
                        if (light instanceof HomeLight) {
                            HomeLight homeLight = (HomeLight) light;
                            System.out.println("(Combination: " + combinationIndex + ")");
                            System.out.println("\n");
                            System.out.println("Setting power for: " + homeLight.getName() + " (Power: " + list.get(ix) + ")");
                            homeLight.setPower(list.get(ix));
                            currentLights.add(homeLight);

                            imageNameBuilder.append((double) list.get(ix) == 0.0D ? "off" : "on");
                        }
                        ix++;
                    }

                    for (HomeFurnitureGroup group : groups) {
                        String groupState = (double) list.get(ix) == 0.0D ? "off" : "on";
                        imageNameBuilder.append(groupState);
                        for (HomePieceOfFurniture light : group.getFurniture()) {
                            if (light instanceof HomeLight) {
                                HomeLight homeLight = (HomeLight) light;
                                System.out.println("Setting power for group light: " + homeLight.getName() + " (Power: " + (groupState.equals("on") ? initialPowerValues.get(homeLight) : 0.0F) + ")");
                                homeLight.setPower((groupState.equals("on") ? initialPowerValues.get(homeLight) : 0.0F));
                                currentLights.add(homeLight);
                            }
                        }
                        ix++;
                    }

                    String imageName = imageNameBuilder.toString();
                    System.out.println("Generating image: " + imageName);

                    ArrayList<HomePieceOfFurniture> itemsToRemove = new ArrayList<>();

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

                    for (HomeLight light : currentLights) {
                        Float initialPower = initialPowerValues.get(light);
                        if (initialPower != null) {
                            light.setPower(initialPower);
                        }
                    }

                    final int progress = combinationIndex;
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress);
                        progressLabel.setText("Images saved: " + progress + " / " + totalImages + " (" + (progress * 100 / totalImages) + "%)");
                    });

                    if (Thread.currentThread().isInterrupted() || worker.isCancelled()) {
                        throw new InterruptedException("Image generation cancelled");
                    }
                }

                for (Map.Entry<HomePieceOfFurniture, Boolean> entry : initialVisibilityValues.entrySet()) {
                    entry.getKey().setVisible(entry.getValue());
                }

            } catch (Exception e) {
                JOptionPane.showInternalMessageDialog(null, "Error: " + e.getMessage());
            }
        }

        // Create an image for a given home state
        private void createImage(Home home, HomeAssistantLightningOptions options, String name) {
            try {
                System.out.println("Generating image " + options.getPath() + "/" + name);
                long millis = System.currentTimeMillis();
                PhotoRenderer renderer = new PhotoRenderer(home, options.getQuality());
                BufferedImage image = new BufferedImage(options.getImageWidth(), options.getImageHeight(), BufferedImage.TYPE_INT_RGB);
                renderer.render(image, home.getCamera(), new ImageObserver() {
                    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
                        if ((infoflags & ImageObserver.ALLBITS) != 0) {
                            System.out.println("Image loaded!");
                            return true;
                        }
                        return false;
                    }
                });

                File outputfile = new File(options.getPath() + "/" + name + ".jpg");
                ImageIO.write(image, "jpg", outputfile);
                System.out.println("Image generated, Time needed:" + (System.currentTimeMillis() - millis));
                System.out.println("-----------------------------------");
            } catch (IOException var9) {
                JOptionPane.showMessageDialog(null, "Error: " + var9.getMessage());
                var9.printStackTrace();
            }
        }

        // Retrieve all lights and groups from the furniture list
        public void getAllHomeLightsAndGroups(List<HomePieceOfFurniture> furnitureList, List<HomePieceOfFurniture> lights, List<HomeFurnitureGroup> groups) {
            for (HomePieceOfFurniture piece : furnitureList) {
                if (piece instanceof HomeLight) {
                    lights.add(piece);
                } else if (piece instanceof HomeFurnitureGroup) {
                    groups.add((HomeFurnitureGroup) piece);
                }
            }
        }

        // Get lights that are not part of any group
        public void getLightsNotInGroups(List<HomePieceOfFurniture> allLights, List<HomeFurnitureGroup> groups, List<HomePieceOfFurniture> lightsNotInGroups) {
            Set<HomePieceOfFurniture> groupedLights = new HashSet<>();
            for (HomeFurnitureGroup group : groups) {
                groupedLights.addAll(group.getFurniture());
            }
            for (HomePieceOfFurniture light : allLights) {
                if (!groupedLights.contains(light)) {
                    lightsNotInGroups.add(light);
                }
            }
        }
    }
}