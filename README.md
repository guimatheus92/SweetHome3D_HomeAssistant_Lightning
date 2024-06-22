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
4. There are 5 fields to be filled before generating the images:
- **Path**: Path where the images will be generated
- **Image Width**: Width of the images to generate (recommended: 380)
- **Image Height**: Height of the images to generate (recommended: 285)
- **Quality**: Image quality (recommended: high)
- **Home Assistant path**: This path is where the images will be located in Home Assistant
5. Move the images from Path to `/config/www/planes`
7. Create a card of type `picture-elements` and paste the generated `.yaml` from Path.

# How to copy `.jar` into Sweet Home 3D

To use it, copy this archive in the plug-ins folder of Sweet Home 3D, which depends on your system as follows:
- under Windows, this folder is `C:\Documents and Settings\user\Application Data\eTeks\Sweet Home 3D\plugins`,
- under Mac OS X, it's the subfolder `Library/Application Support/eTeks/Sweet Home 3D/plugins` of your user folder,
- under Linux and other Unix, it's the subfolder `.eteks/sweethome3d/plugins` of your user folder.