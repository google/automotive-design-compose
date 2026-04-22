const esbuild = require('esbuild');
const fs = require('fs');
const path = require('path');


const uiHtmlPath = path.join(__dirname, 'src/ui.html');
const codeJsPath = path.join(__dirname, 'src/code.ts');
const distDir = path.join(__dirname, 'dist');
const outputUiHtmlPath = path.join(distDir, 'ui.html');
const manifestPath = path.join(__dirname, 'manifest.json');
const distManifestPath = path.join(distDir, 'manifest.json');
const manualPath = path.join(__dirname, 'MANUAL.md');
const distManualPath = path.join(distDir, 'MANUAL.md');

// Create dist directory if it doesn't exist
if (!fs.existsSync(distDir)) {
    fs.mkdirSync(distDir);
}

// Copy MANUAL.md to dist
fs.copyFileSync(manualPath, distManualPath);
console.log('- Copied MANUAL.md to dist.');

// Read manifest.json, modify paths, and write to dist
const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf-8'));
manifest.main = 'code.js';
manifest.ui = 'ui.html';
fs.writeFileSync(distManifestPath, JSON.stringify(manifest, null, 2));
console.log('- Copied and modified manifest.json to dist.');



console.log('Bundling assets with esbuild...');
const tempUiEntryPath = path.join(__dirname, 'src/temp-ui-entry.ts');
const uiJsOutputPath = path.join(distDir, 'ui.js');

// Create a temporary entry file for ui.js
const timelineFiles = fs.readdirSync(path.join(__dirname, 'src/timeline')).filter(fn => fn.endsWith('.ts'));
const timelineImports = timelineFiles.map(fn => `import './timeline/${fn}';`).join('\n');
fs.writeFileSync(tempUiEntryPath, `import './ui.ts';
${timelineImports}
`);

const tempTimelineEntryPath = path.join(__dirname, 'src/temp-timeline-entry.ts');
fs.writeFileSync(tempTimelineEntryPath, timelineFiles.map(file => `export * from './timeline/${file}';`).join('\n'));

const timelineEditorBuild = esbuild.build({
    entryPoints: [tempTimelineEntryPath],
    bundle: true,
    outfile: path.join(distDir, 'timeline-editor.js'),
    globalName: 'TimelineEditor',
    format: 'iife'
}).then(() => {
    console.log('- Bundling timeline-editor.js complete.');
    fs.unlinkSync(tempTimelineEntryPath);
});

const stylesPath = path.join(__dirname, 'src/timeline/styles.css');
const distStylesPath = path.join(distDir, 'styles.css');
fs.copyFileSync(stylesPath, distStylesPath);
console.log('- Copied styles.css to dist.');

Promise.all([
    timelineEditorBuild,
    // Bundle ui.js
    esbuild.build({
        entryPoints: [tempUiEntryPath],
        bundle: true,
        outfile: uiJsOutputPath,
    }).then(() => {
        console.log('- Bundling ui.js complete.');
        // Read the bundled script
        const bundledJs = fs.readFileSync(uiJsOutputPath, 'utf-8');

        // Read ui.html and inline the script
        console.log('- Inlining ui.js into ui.html');
        let uiHtml = fs.readFileSync(uiHtmlPath, 'utf-8');
        uiHtml = uiHtml.replace('<script src="ui.ts"></script>', `<script>${bundledJs}</script>`);

        // Inline styles
        const styles = fs.readFileSync(stylesPath, 'utf-8');
        uiHtml = uiHtml.replace('</head>', `<style>${styles}</style></head>`);

        // Write the final ui.html to dist
        fs.writeFileSync(outputUiHtmlPath, uiHtml);
        console.log('- Finished processing ui.html.');

        // Clean up the temporary entry file
        fs.unlinkSync(tempUiEntryPath);
    }),
    // Bundle code.js
    esbuild.build({
        entryPoints: [codeJsPath],
        bundle: true,
        outfile: path.join(distDir, 'code.js'),
        platform: 'browser',
        target: 'es2019',
    }).then(() => {
        console.log('- Bundling code.js complete.');
    })
]).then(() => {
    console.log('Build successful!');
}).catch((e) => {
    console.error('Build failed:', e);
    // Clean up the temporary entry file in case of an error
    if (fs.existsSync(tempUiEntryPath)) {
        fs.unlinkSync(tempUiEntryPath);
    }
    if (fs.existsSync(tempTimelineEntryPath)) {
        fs.unlinkSync(tempTimelineEntryPath);
    }
    process.exit(1);
});
