# Sweet Home 3D Export Images

This plugin was inspired from https://github.com/sergiocasero/sweet_home_3d_ha_lightning, kudos to *sergiocasero*.

The goal of this plugin is to generate all possible combinations of images of a floor plan created in Sweet Home 3D. This allows for real-time visualization of the status of lights in Home Assistant.

To achieve this, we need to generate a YAML file with the structure of [picture-elements](https://www.home-assistant.io/dashboards/picture-elements/ "picture-elements") and create all possible combinations of images based on the lights. For example, if we have three lights: "living_room," "kitchen," and "bed_room," we need to generate:

| light.living_room | light.kitchen | light.bedroom |
| ------------ | ------------ | ------------ |
| 0 | 0 | 0 |
| 0 | 0 | 1 |
| 0 | 1 | 0 |
| 0 | 1 | 1 |
| 1 | 0 | 0 |
| 1 | 0 | 1 |
| 1 | 1 | 0 |
| 1 | 1 | 1 |

As we can see, for 3 lights, there are 2^3 possible combinations. If we have 10 lights, there would be 2^10 combinations, resulting in 1024 images... and we don't want to generate all the **images manually**!

# Features

- **Automatic Image Generation**: Automatically generate all possible combinations of images for a floor plan created in Sweet Home 3D based on the status of lights.
- **Real-Time Visualization**: Instantly visualize the status of lights in Home Assistant using the generated images, no need to do it manually.
- **YAML Configuration**: Generate a YAML file with the picture-elements structure for easy integration with Home Assistant.
- **Scalable Solution**: Efficiently handle multiple lights, including lights that are under Furniture Groups in Sweet Home 3D, generating combinations automatically without manual intervention.
- **Loading Bar**: Visual loading bar to track the progress of image generation, ensuring users are informed about the process and its completion status.

# How to activate plugin:
1. Download the latest release in `.jar` and copy to Sweet Home 3D "plugins" folder (see notes below) or download the latest release in `.sh3p` and execute that file.

> 	If double-clicking on a `.sh3p` file doesn't launch Sweet Home 3D (most chances under Linux), you can also install a plug-in with the following command in a Terminal window (where SweetHome3D is the name of the executable file provided with Sweet Home 3D installers): `/path/to/SweetHome3D /path/to/plugin.sh3p`

2. Give the lights in Sweet Home 3D, the same name as in Home Assistant entities, for example: light.living_room. That way you will automate the name generation as well.
3. Go to `Tools` in Sweet Home 3D -> Home Assistant Lightning Export
4. Move the generated images from Path to `/config/www/planes`
5. Create a card of type `picture-elements` and paste the generated `.yaml` from Path.

# How to copy `.jar` into Sweet Home 3D

To use it, copy this archive in the plug-ins folder of Sweet Home 3D, which depends on your system as follows:
- under Windows, this folder is `C:\Documents and Settings\user\Application Data\eTeks\Sweet Home 3D\plugins`,
- under Mac OS X, it's the subfolder `Library/Application Support/eTeks/Sweet Home 3D/plugins` of your user folder,
- under Linux and other Unix, it's the subfolder `.eteks/sweethome3d/plugins` of your user folder.

# How to use it:

1. Click on `Tools` in Sweet Home 3D top menu and select the plugin name:

![alt text](/media/plugin.png)

2. Now fill the values in the opened box as described below:

- **Path**: Path where the images will be generated
- **Image Width**: Width of the images to generate
- **Image Height**: Height of the images to generate
- **Quality**: Image quality (recommended: high)
- **Home Assistant path**: This path is where the images will be located in Home Assistant

![alt text](/media/fill-values.png)

3. After you click on `OK`, it will open a box telling you how many lights without groups, groups with lights how many images (combinatios) will be generated:

> 	Note that a group of light is considered as a single entity, so when that group is iterated in the loop all lights within that group will be set as on/off at the same time. We did this to decrease the number of images generated.
For example, in my home I have 60 lights if I do not group them it would be generated 1,152,921,504,606,846,976 images, which is non sense.

![alt text](/media/total-entities.png)

4. If you confirm the previous information, then a progress bar will be shown to the user with the generation information:

![alt text](/media/progress-bar.png)

When that process is finished, it will then show the elapsed end time.

5. When that process is finished, you will get images generated with entities states as images names, `.yaml` file to use in Home Assistant and `log.txt` file that will capture the information that was generated from plugin.

> 	This `log.txt` file you won't use anywhere, is just for debugg purpose and test only.

![alt text](/media/images-generated.png)