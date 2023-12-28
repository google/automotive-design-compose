## Building the widget

Below are the steps to build your widget. You can also find instructions at:

https://www.figma.com/widget-docs/setup-guide/

This widget template uses Typescript and NPM, two standard tools in creating JavaScript applications.

First, download Node.js which comes with NPM. This will allow you to install TypeScript and other
libraries. You can find the download link here:

https://nodejs.org/en/download/

Or you can install on linux with the command:
`sudo apt-get install nodejs npm git`

Next, run the following to build the widget:
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

## Adding the widget to Figma

To add the development version of the widget, you need to be running the Figma desktop application on a Windows or MacOS machine.

- In a doc, right-click and open the widget menu.
- Select "Development"
- Select "Import widget from manifest"
- Navigate to your `manifest.json` and press OK.
- Once you've done this once, the widget will be available directly in the "Development" menu option.

## Network access restrictions

> [!TIP]
> You can use the `devAllowedDomains` field to define domains that you need
> access to for development purposes.

The [manifest.json](manifest.json) file defines an allowlist of domains that the
widget is permitted to load [cross-origin resources] from. Any changes that
introduce dependencies on origins not covered by the existing allowlist will
need to update the [networkAccess] field in the widget's manifest file.

[cross-origin resources]: https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS
[networkAccess]: https://www.figma.com/widget-docs/widget-manifest/#networkaccess
