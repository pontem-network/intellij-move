# Move Language plugin for the Intellij platform

## Installation

Open `Settings > Plugins > Marketplace` in your IDE, search for _Move Language_ and install the plugin.
To open an existing project, use **File | Open** and point to the directory containing `Move.toml`. 

New features are announched in [changelogs](https://github.com/pontem-network/intellij-move/tree/master/changelog). 

## Features

* Syntax highlighting
* Code formatting
* Go-to-definition
* Rename refactoring
* Type inference
* `Move.toml` and `move` binary integration

## Dependencies

For git dependencies specified in `Move.toml` file you need to manually run `move package build` to populate `build/` directory. 

## Compatible IDEs

All Intellij-based IDEs starting from version 2021.1. For 2020.3 and below you can use old versions of the plugin. 
