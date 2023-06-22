# Design Compose Jekyll Site

Our documentation is written in markdown and turned into a static website using Jekyll. The site is currently hosted on GitHub Pages.

## Running the site locally

Jekyll is a Ruby Gem and requires Ruby to run. The recommended way to install Ruby is to use `rbenv`.

- Follow steps 1, 2 and 3 of the [Basic Git Checkout](https://github.com/rbenv/rbenv#basic-git-checkout) section of the `rbenv` docs. (Don't install from apt-get, that version is very out of date.)
- Switch to this directory (`/docs`), which contains a .ruby-version file. 
- Run `rbenv install` to install the correct version of Ruby

I recommend also following the instructions for the [jekyll-github-metadata](https://github.com/jekyll/github-metadata/blob/main/docs/authentication.md) plugin to set up a GitHub token. Otherwise you may run into rate limiting from GitHub while re-building the site.

To start the site:

- Switch to this directory (`docs`)
- Run `bundle install` to install the required Gems (usually only need to do this once)
- Run `bundle exec jekyll serve -wIl`
    - -w to watch for changes and rebuild, -I for incremental rebuild and -l to enable live reload in your browser

If the site builds correctly it will start hosting it at something like `127.0.0.1:4000` (check the output of the `serve` command for the actual address). Open that in your local web browser. (You can also add `-o` to automatically open the site in your browser)

## Content

The main content is located in `docs/_docs`. The underscore means it can be processed as a collection, which allows it to be organized for the sidebar under a "Documentation" header (More info)[https://just-the-docs.com/docs/configuration/#document-collections]. Anything that isn't "documentation" (like an "About" page or something else) can be located inside the main `docs` folder, and it will appear in the sidebar alongside the other uncollected pages like "Home"

### Internal linking

Jekyll's [link](https://jekyllrb.com/docs/liquid/tags/#links) tag is used to generate the proper path to the file you want to link to. Any anchors need to be after the link tag. For example, a link to the Store Figma Token section of the Live Update Setup page would be: 

```
[Link text]({%link _docs/live-update/setup.md %}#StoreFigmaToken)
```

### Theme and Styling

The theme used is (Just the Docs)[https://just-the-docs.com/]. See the theme's page for instructions on making changes to the colors and features. You can click the "Edit this page in GitHub" link at the bottom of any page to see the source for it, if you need any inspiration.