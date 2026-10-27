---
Title: Tips and Tricks
nav_order: 15
layout: page
---

# Tips and Tricks

(More to come!)

## Compile time optimizations

Try to separate your `@DesignDoc()` declarations into individual files. The
declarations must be reprocessed and recompiled any time a change is detected in
it's containing file, so minimizing the amount of code that could change will
minimize the amount of time spent recompiling your source.

## DCF File Info

When live update downloads a Figma design, the file is saved into a DesignCompose
file (.dcf) and cached on the device. This file has a version associated with it
and cannot be used with a different version of DesignCompose. To view the version
along with some other data in the .dcf file, use the `dcf_info` tool. From the
root of the DesignCompose repository, run:
```
cargo run --bin dcf_info --features=dcf_info <dcf file>
```

This tool attempts to print the following fields. In the scenario in which the .dcf
file is too old and cannot be parsed, only the `DC Version` field will be printed:

| Field    | Description |
| -------- | ------- |
| DC Version    | The version of DesignCompose that saved this file  |
| Doc ID        | The Figma document ID this file was retrieved from |
| Figma Version | The version ID of the file from Figma's API        |
| Name          | The name of the document in Figma                  |
| Last Modified | The time the Figma file was last modified          |

For example:
```
$ cargo run --bin dcf_info --features=dcf_info hello.dcf
  DC Version: 23
  Doc ID: pxVlixodJqZL95zo2RzTHl
  Figma Version: 2151334464145538330
  Name: Hello World
  Last Modified: 2024-11-16T01:29:05Z
```

## Custom Fonts Configuration {#custom-fonts}

Figma documents often use custom typefaces (such as Google Fonts or brand-specific font families). By default, Android text rendering falls back to system fonts if the specified Figma font family is not registered.

To configure custom fonts in DesignCompose:

1. **Place Font Resources:**
   Add your font `.ttf` or `.otf` files to your Android application module's `res/font/` directory (e.g., `res/font/inter_regular.ttf`, `res/font/inter_bold.ttf`).

2. **Define Compose `FontFamily`:**
   Create a Compose `FontFamily` referencing your font resources with their respective weights and styles:

   ```kotlin
   import androidx.compose.ui.text.font.Font
   import androidx.compose.ui.text.font.FontFamily
   import androidx.compose.ui.text.font.FontWeight
   import androidx.compose.ui.text.font.FontStyle

   val interFont = FontFamily(
       Font(R.font.inter_regular, FontWeight.Normal, FontStyle.Normal),
       Font(R.font.inter_medium, FontWeight.Medium, FontStyle.Normal),
       Font(R.font.inter_bold, FontWeight.Bold, FontStyle.Normal),
       Font(R.font.inter_italic, FontWeight.Normal, FontStyle.Italic),
   )
   ```

3. **Register with `DesignSettings.addFontFamily`:**
   In your `Application.onCreate()` or `Activity.onCreate()` (before rendering any `@DesignDoc` composable), register the font family mapping:

   ```kotlin
   import com.android.designcompose.DesignSettings

   class MainActivity : ComponentActivity() {
       override fun onCreate(savedInstanceState: Bundle?) {
           super.onCreate(savedInstanceState)

           // Register custom font family matching the exact Figma font name
           DesignSettings.addFontFamily("Inter", interFont)
           DesignSettings.addFontFamily("Cedarville Cursive", cursiveFont)

           setContent {
               MyDesignDoc.MainFrame()
           }
       }
   }
   ```

   The first argument to `DesignSettings.addFontFamily` must match the font family name used in Figma.
