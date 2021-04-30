# Move Language plugin for Intellij platform

https://plugins.jetbrains.com/plugin/14721-move-language

## Features

* Syntax highlighting
* Code formatting
* Go-to-definition
* Rename refactoring
* [Dove](https://docs.pontem.network/03.-move-vm/compiler_and_toolset#dove) integration

## Installation

Open `Settings > Plugins > Marketplace` in your IDE, search for _Move Language_ and install the plugin.

To enable multifile support you need to set up Dove. For that you need to:

1. Download the latest binary for your platform from the Dove repository [https://github.com/pontem-network/move-tools/releases](https://github.com/pontem-network/move-tools/releases). 
2. Add its filesystem path to the `Settings -> Languages & Frameworks -> Move -> Dove executable`.
3. Initialize your project root with `dove init`. This will create the following file structure:

```
    /scripts
    /modules
    /tests
    Dove.toml
```

For more information of Dove and Dove.toml file
see [https://docs.pontem.network/03.-move-vm/compiler_and_toolset#dove](https://docs.pontem.network/03.-move-vm/compiler_and_toolset#dove)

## Compatible IDEs

All Intellij-based IDEs starting from version 2021.1. For 2020.3 and below you can use old versions of the plugin. 
