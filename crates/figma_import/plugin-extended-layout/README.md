# vsw Extended Layout

This plugin adds some additional controls for text and flexbox containers, and copies interactive properties from Figma's JS API to a per-node storage area that Figma's REST API can read (until Figma's REST API provides interaction data). It uses a Figma UI2 stylesheet from https://github.com/thomas-lowry/figma-plugin-ds to look more like Figma.

For text, the plugin adds controls that can set:
 * The maximum line count (or zero for unlimited lines).
 * Whether the text should be ellipsized.

For autolayout containers, we add a control for them to wrap their children. This corresponds to the flexbox "flex-wrap" property. This property is useful for making grids, although because Figma itself doesn't support "flex-wrap", you don't get to see the wrapping in Figma.

This plugin also provides a menu item to "Sync Interactive", which simply copies the interactive fields from the JS API to a data area that the REST API can fetch (since our webservice can't talk to the JS API).

## Building the plugin
Install Linux dependencies:
`sudo apt-get install nodejs npm git`

Run the plugin build:
`npm install`
`npm run build`

Copy the built plugin to your macOS/Windows machine; you just need:
`manifest.json`, `ui.html`, and `code.js` in a directory somewhere.

Add the plugin to your local Figma:
 * In a doc, right-click and open the plugins menu.
 * Select "Manage Plugins"
 * Press the "+" next to "In Development"
 * Navigate to your `manifest.json` and press OK.
