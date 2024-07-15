## Plugin Features

This plugin adds some additional capabilities to Figma specifically for Automotive Design for Compose. It writes additional data into Figma nodes using Figma's JS API that can be read from Figma's REST API. It uses a Figma UI2 stylesheet from <https://github.com/thomas-lowry/figma-plugin-ds> to look more like Figma. There are five plugin menu options, detailed below.

- Live text format: Additional controls to set:
  - The maximum line count (or zero for unlimited lines).
  - Whether the text should be ellipsized.
- Dials and Gauges: Configure nodes to act as a type of dial or gauge
- Check document for errors: Using a JSON file created from the application's DesignCompose annotations, check for possible errors in the document such as missing or incorrectly named nodes
- Check/update keywords: Upload a new JSON file created from the application's DesignCompose annotations
- Sync Prototype changes: Copies the interactive fields from the JS API to a data area that the REST API can fetch (since our webservice can't talk to the JS API). This needs to be run anytime an interaction changes.
- Localization: Assigns each text node a string resource name and generates a strings xml file to integrate with translations for the Android app.

## Building the plugin

Below are the steps to build your plugin. You can also find instructions at:

https://www.figma.com/plugin-docs/

This plugin uses Typescript and NPM, two standard tools in creating JavaScript applications.
https://www.figma.com/plugin-docs/

First, download Node.js which comes with NPM. This will allow you to install TypeScript and other
libraries. You can find the download link here:

https://nodejs.org/en/download/

Or you can install on linux with the command:
`sudo apt-get install nodejs npm git`

Next, run the following to build the plugin:
`npm install`
`npm run build`

If you are familiar with JavaScript, TypeScript will look very familiar. In fact, valid JavaScript code
is already valid Typescript code.

TypeScript adds type annotations to variables. This allows code editors such as Visual Studio Code
to provide information about the Figma API while you are writing code, as well as help catch bugs
you previously didn't notice.

For more information, visit https://www.typescriptlang.org/

Using TypeScript requires a compiler to convert TypeScript (code.ts) into JavaScript (code.js)
for the browser to run.

We recommend writing TypeScript code using Visual Studio code:

1. Download Visual Studio Code if you haven't already: https://code.visualstudio.com/.
2. Open this directory in Visual Studio Code.
3. Compile TypeScript to JavaScript: Run the "Terminal > Run Build Task..." menu item,
   then select "npm: watch", which executes the command "npm run build -- --watch". You will have to do this again every time
   you reopen Visual Studio Code.

That's it! Visual Studio Code will regenerate the JavaScript file every time you save.
Add the plugin to your local Figma:

## Running the plugin in Figma

To run the development version of the plugin, you need to be running the Figma desktop application on a Windows or MacOS machine.

- In a doc, right-click and open the plugins menu.
- Select "Development"
- Select "Import plugin from manifest"
- Navigate to your `manifest.json` and press OK.
- Once you've done this once, the plugin will be available directly in the "Development" menu option.

## Network access restrictions

> [!TIP]
> You can use the `devAllowedDomains` field to define domains that you need
> access to for development purposes.

The [manifest.json](manifest.json) file defines an allowlist of domains that the
plugin is permitted to load [cross-origin resources] from. Any changes that
introduce dependencies on origins not covered by the existing allowlist will
need to update the [networkAccess] field in the plugins's manifest file.

[cross-origin resources]: https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS
[networkAccess]: https://www.figma.com/plugin-docs/manifest/#networkaccess
