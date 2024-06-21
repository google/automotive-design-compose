# DesignCompose protobuf message repository

The messages in this directory are organized according to the following principles:

```ascii-tree https://github.com/aprilandjan/vscode-ascii-tree-generator
.
├── android_interface - Types specifically used for the interface from the rust library to the android library
├── definition - The structure for a DesignCompose Definition
│   ├── element - "Base" types. Messages in element should not reference messages outside of element
│   ├── interaction - Types related to interactions and reactions
│   ├── layout - Layout and positioning
│   ├── modifier - Transformations, blending, other modifiers that can be applied to a base view
│   ├── plugin - Capabilities added by the DesignCompose plugin
│   └── view - Collected styles and settings that are applied to nodes in the design definition
└── live_update - Types that are specific to the live update system and do not influence the design definition
    └── figma - These types are specific to the live update implementation for Figma
```
