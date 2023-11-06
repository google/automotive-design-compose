# Releasing

If it doesn't already exist, create a `stable/` branch for the new version. Create a new GitHub release from that `stable/` branch with a tag in this format: "v.\*". This will trigger the Release action which will build the libraries and Figma resources using the version number set in the tag. Once successful those artifacts will be uploaded to the GitHub release.

## Publishing the SDK libraries
Upload the Maven repo to gmaven using gmaven_publisher (go/gmaven)

## Publishing the Figma Artifacts

### Figma resources
Download the Figma resources (widget and plugin) on a Windows/Mac system with Figma Desktop. Open a doc in Figma Desktop, click the **Resources** dropdown and select "Plugins". Change the dropdown from "Recents" to "Development". If you already have an entry for the published Plugin (it will have a 🌐 next to it) then open the `...` menu and click "Remove local version". Then click "Locate local version" and select the manifest.json for the downloaded file. Then click the `...` menu again and select Publish new version. You can set release notes if you feel it's necessary, then click "Publish". Do the same for the widget.

### Figma files (Tutorial, etc)
Open the file to publish, and click "Share" (upper right hand corner). In the box, switch to the "Publish" tab and then "Publish update...".
