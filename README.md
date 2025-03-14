# <img src="logos/logo.png" alt="Allium Logo" width="300"/>

[![Github Stars](https://img.shields.io/github/stars/moongardenmods/allium?color=blue&label=Stars&logo=github&style=flat)](https://github.com/moongardenmods/allium/stargazers) 
[![Allium Modrinth Downloads](https://img.shields.io/modrinth/dt/allium?color=00AF5C&label=allium&style=flat&logo=modrinth)](https://modrinth.com/mod/allium)
[![Bouquet Modrinth Downloads](https://img.shields.io/modrinth/dt/bouquet?color=00AF5C&label=bouquet&style=flat&logo=modrinth)](https://modrinth.com/mod/bouquet)

Lua script loader for Java Minecraft.

Currently, only functioning and in development in Fabric, with the intent of supporting Forge/Quilt later on down the 
road.

## Installing
Allium installs just like any other Fabric mod. Depending on the scripts you use, they may depend on Bouquet, 
an additional mod that adds libraries to make scripting easier. Much like Fabric API, this mod is independent and not 
built into the loader side of Allium and must be downloaded individually.

If you'd like to use Allium or Bouquet to extend functionality of your own Java mod, publications can be found on
[hugeblank's maven](https://maven.hugeblank.dev/#/releases/dev/hugeblank).

### Scripts
In the same place that you create your `mods` directory, create a folder named `allium`. This is where your lua scripts 
will go. They may come in the form of a `.zip`, but may be expanded, and put into a regular directory if you'd like to 
tinker with them. If you're at all familiar with the Resource/Data Pack structure, then you'll know that this is 
similar.

## Limitations
Allium aims to be a gateway into modding, however there are currently some limitations that stifle its utility. Notably,
mixins and documentation. There was an attempt to get documentation that a Lua user can approach to understand Java 
logic, but it wasn't particularly elegant, nor complete. Mixin support on the other hand is in progress, slowly. Both of 
these limitations will be resolved before version 1.0. While there is not documentation, examples of how scripting works
can be found in [`bouquet/examples`](bouquet/examples).

## Logos
Allium's logos are under the same license as the rest of the project. Feel free to use these in your own project.

<!--suppress CheckImageSize -->
<img src="logos/icon.png" alt="Allium Icon" height="100"/> 
<img src="logos/logo.png" alt="Allium Logo" height="100"/> 
<img src="logos/banner.png" alt="Powered by Allium" height="100"/>

## Contributing
Allium is broken up into 2 gradle subprojects that each build into their own jars. When making a pull request please
make sure to use the template that corresponds to which project you're contributing to (TODO). If you'd like to discuss
contribution please feel free to join the [Moongarden mods discord](https://discord.gg/rWSaP222G9).

### Allium
Found in the `allium` directory, this is the bare-minimum necessary for a Lua script to be run in the game.

### Bouquet
Found in the `bouquet` directory, this features additional quality of life libraries, as well as frequently used
event hooks into the games logic.

### Dev Environment Notes
- To build both projects at the same time use the `buildAll` gradle task.
- Multiple client run configurations are created. One is for running only allium, the other is for running both allium & bouquet.

## Too much Allium...
Since 2018 a project under the name Allium has existed. This is being addressed to suppress the Mandela Effect.

### Isn't Allium for ComputerCraft?
Allium for CC has been moved to [allium-cc](https://github.com/hugeblank/allium-cc). This project took its original repository location due to it being a 
far more useful successor to the CC variant.

### Isn't Allium a peripheral mod for ComputerCraft?
That's Allium Peripherals. You can check it out [here](https://github.com/hugeblank/allium-peripherals).
