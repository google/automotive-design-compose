/**
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const path = require("path");
const webpack = require("webpack");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const HTMLInlineCSSWebpackPlugin =
  require("html-inline-css-webpack-plugin").default;
const CopyPlugin = require("copy-webpack-plugin");

module.exports = (env, argv) => ({
  mode: argv.mode === "production" ? "production" : "development",

  // This is necessary because Figma's 'eval' works differently than normal eval
  devtool: argv.mode === "production" ? false : "inline-source-map",
  entry: {
    code: "./src/code.ts", // This is the entry point for our plugin code.
    css: path.resolve(__dirname, "./src/style.css"), // Our stylesheet that will be inlined
  },
  module: {
    rules: [
      // Converts TypeScript code to JavaScript
      {
        test: /\.tsx?$/,
        use: "ts-loader",
        exclude: /node_modules/,
      },
      // Enables extracting CSS and inlining into <style>
      {
        test: /\.css$/,
        use: [MiniCssExtractPlugin.loader, "css-loader"],
      },
      // Allows you to use "<img src=file.svg>" in your HTML code, and then inlines the
      // image in the out put HTML file.
      {
        test: /\.(png|jpg|svg)$/i,
        type: "asset/inline",
      },
      // Allows html-loader to look for image references
      {
        test: /\.html$/i,
        use: "html-loader",
      },
    ],
  },
  // Webpack tries these extensions for you if you omit the extension like "import './file'"
  resolve: {
    extensions: [".tsx", ".ts", ".jsx", ".js"],
  },
  output: {
    filename: "[name].js",
    path: path.resolve(__dirname, "dist"),
  },
  plugins: [
    new MiniCssExtractPlugin({
      filename: "[name].css",
      chunkFilename: "[id].css",
    }),
    new HTMLInlineCSSWebpackPlugin(),
    // Process the ui html file in addition to typescript files
    new HtmlWebpackPlugin({
      template: "src/ui.html",
      filename: "ui.html",
      inject: false,
    }),
    new HtmlWebpackPlugin({
      template: "src/shader.html",
      filename: "shader.html",
      inject: false,
    }),
    new HtmlWebpackPlugin({
      template: "src/scalable.html",
      filename: "scalable.html",
      inject: false,
    }),
    new CopyPlugin({
      patterns: [
        { from: "src/data/shader/", to: "data/shader/" }, // Copy to the dist folder
      ],
    }),
    new CopyPlugin({
      patterns: [
        {
          from: "../../third_party/shader_examples/",
          to: "data/shader/",
          filter: async (filePath) => {
            return filePath.endsWith(".txt");
          },
        }, // Copy to the dist folder
      ],
    }),
  ],
});
