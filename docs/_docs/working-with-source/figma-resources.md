---
title: Figma Resources
layout: page
parent: Working with Source
nav_order: 2
---

# Figma Resources

The Figma plugin enables various features that enhance the tools that Figma
provides. The Auto Content Preview widget adds even more ways to see how your
lists render with various parameters. The latest releases are available in the
Figma Community and on our [Figma Community profile][1]{:.external}.

If you're using an older version of the Automotive Design for Compose SDK then
it might be necessary to install a previous or customized version of the plugin
or widget. If so, follow the instructions on this page.

## Install NodeJS {#InstallNodeJS}

NodeJS is required to compile the Figma resources. On Linux it can be installed
by running `apt-get install nodejs npm`.

## Install a previous or customized versions {#install}

Note: The plugin and widget need to be installed only once per Figma
organization. If your account is part of an organization, then you can check to
see whether they're already installed in the Resources tab of the editor. If so,
skip this section.

Important: Plugins and widgets must be installed with the [Figma desktop
app][2]{:.external}, which only runs on macOS and Windows.

The plugin is located in the `support-figma/extended-layout-plugin/` directory
of the [DesignCompose source][3]. The widget is located in the
`support-figma/auto-content-preview-widget/` directory.

### Create new resource IDs {#create-new-ids}

Figma uses unique IDs for each plugin and widget. Before uploading your own
version of a resource you need to create a new ID for it.

1.  Follow [Create a plugin for development][4]{:.external} to create a new
    empty plugin template.

1.  Open the template's `manifest.json` file and copy the `id` instance.

1.  Open the `manifest.json` file for the DesignCompose widget or plugin and
    replace the existing `id` instance with the one from the template.

### Build the resources {#building-resources}

Build each by running the following:

```posix-terminal
npm install

npm run build
```

This produces a `code.js` file. That file, the `manifest.json` file, and the
`ui.html` file are needed to install the plugin.

### Upload the resources {#upload}

You can install the DesignCompose Figma plugin and widget in the Figma desktop
app on macOS and Windows. Download the Figma app from [Figma
downloads][2]{:.external}.

Install the Figma resources by following these steps:

1.  Open the Figma app and open any Figma document or create a new one.

1.  Open the **Menu** (the Figma logo in the upper left) and select
    **Plugins** > **Development** >
    **Import plugin From manifest**.

1.  Click **Find manifest.json**, go to the build output of the
    `support-figma/extended-layout-plugin/` directory, and select the
    `manifest.json` file.

1.  Go to **Menu** > **Widgets** > **Development** > **Import widget from
    manifest**.

1.  Click **Find manifest.json**, go to the build output of the
    `support-figma/auto-content-preview-widget/` directory, and select the
    `manifest.json` file.

When installed, the Figma plugin is listed in the Figma context menu under
**Plugins** > **Development** whenever a document is open.

To keep everyone in an organization on the same version of the plugin, we
recommend your Figma administrator publish the plugin to your Figma
organization. Doing so means only one person needs to extract the plugin when an
update is released.

To learn of new and updated plugins, see the [Figma Community
plugins][6]{:.external} website.

[1]: https://www.figma.com/@designcompose
[2]: https://www.figma.com/downloads/
[3]: /docs/working-with-source/building-sdk/#GetSource
[4]: https://help.figma.com/hc/en-us/articles/360042786733-Create-a-plugin-for-development
[6]: https://www.figma.com/community/plugins
