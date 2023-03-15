/*
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

package com.android.designcompose.reference.homecompose

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.DesignSettings
import com.android.designcompose.ImageReplacementContext
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant
import com.android.designcompose.reference.media.BrowseItemType
import com.android.designcompose.reference.media.CurrentlyPlaying
import com.android.designcompose.reference.media.MediaAdapter
import com.android.designcompose.reference.media.NavButtonType
import com.android.designcompose.reference.media.PageHeaderType
import com.android.designcompose.reference.media.PlayState
import com.android.designcompose.reference.media.SourceButtonType

val interFont =
  FontFamily(
    Font(R.font.inter_black, FontWeight.Black),
    Font(R.font.inter_blackitalic, FontWeight.Black, FontStyle.Italic),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_bolditalic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
    Font(R.font.inter_extrabolditalic, FontWeight.ExtraBold, FontStyle.Italic),
    Font(R.font.inter_extralight, FontWeight.ExtraLight),
    Font(R.font.inter_extralightitalic, FontWeight.ExtraLight, FontStyle.Italic),
    Font(R.font.inter_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_mediumitalic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_semibolditalic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.inter_thin, FontWeight.Thin),
    Font(R.font.inter_thinitalic, FontWeight.Thin, FontStyle.Italic),
  )

// https://www.figma.com/file/DrKipr7nAGLEp7TCDCDd6f/Home-Compose?node-id=0%3A1
@DesignDoc(id = "DrKipr7nAGLEp7TCDCDd6f")
interface CenterDisplay : com.android.designcompose.reference.media.MediaInterface {
  @DesignComponent(node = "#google/assistant/suggestion")
  fun GoogleAssistantSuggestion(
    @Design(node = "#title") title: String,
    @Design(node = "#icon") icon: Bitmap?,
    @Design(node = "#google/assistant/suggestion") onTap: Modifier,
  )
  @DesignComponent(node = "#google/assistant/shortcut")
  fun GoogleAssistantShortcut(
    @Design(node = "#title") title: String,
    @Design(node = "#icon") icon: Bitmap?,
    @Design(node = "#google/assistant/shortcut") onTap: Modifier,
  )
  @DesignComponent(node = "#home/launcher-icon")
  fun LauncherIcon(
    @Design(node = "#title") title: String,
    @Design(node = "#icon") icon: Bitmap?,
    @Design(node = "#home/launcher-icon") onTap: Modifier,
  )
  @DesignComponent(node = "#home/properties/frame")
  fun CarPropertiesFrame(
    @Design(node = "#home/car-properties/range") range: String,
    @Design(node = "#home/car-properties/range-units") rangeUnits: String,
    @Design(node = "#home/car-properties/charge") charge: String,
    @Design(node = "#home/car-properties/charge-units") chargeUnits: String,
    @Design(node = "#home/car-properties/odometer") odometer: String,
    @Design(node = "#home/car-properties/odometer-units") odometerUnits: String
  )
  @DesignComponent(node = "#home/launchers/frame")
  fun AppLaunchersFrame(@Design(node = "#home/launchers") content: @Composable () -> Unit)
  @DesignComponent(node = "#home/google/assistant/frame")
  fun GoogleAssistantFrame(
    @Design(node = "#google/assistant/suggestion-list") suggestions: @Composable () -> Unit,
    @Design(node = "#google/assistant/shortcut-list") shortcuts: @Composable () -> Unit,
  )
  @DesignComponent(node = "#home/maps/dimmer") fun MapsDimmer()
  @DesignComponent(node = "#Root")
  fun MainFrame(
    // Now Playing
    @Design(node = "#media/now-playing/title") title: String,
    @Design(node = "#media/now-playing/subtitle") artist: String,
    @Design(node = "#media/now-playing/album") album: String,
    @Design(node = "#media/now-playing/artwork")
    albumArt: @Composable (ImageReplacementContext) -> Bitmap?,
    @Design(node = "#media/now-playing/source-icon") appIcon: Bitmap?,
    @Design(node = "#media/now-playing/source-name") appName: String,
    @Design(node = "#media/now-playing/time-elapsed") timeElapsed: String,
    @Design(node = "#media/now-playing/time-duration") timeDuration: String,
    @Design(node = "#media/now-playing/progress-bar")
    progress: @Composable (ComponentReplacementContext) -> Unit,

    // Playback controls
    @DesignVariant(property = "#media/now-playing/play-state-button") playState: PlayState,
    @Design(node = "#media/now-playing/play-state-button") onPlayPauseTap: Modifier,
    @Design(node = "#media/now-playing/skip-prev-button") onPrevTap: Modifier,
    @Design(node = "#media/now-playing/skip-prev-button") showPrev: Boolean,
    @Design(node = "#media/now-playing/skip-next-button") onNextTap: Modifier,
    @Design(node = "#media/now-playing/skip-next-button") showNext: Boolean,
    @Design(node = "#media/now-playing/custom-buttons/auto-content")
    customActions: @Composable () -> Unit,

    // Error frame and authentication button
    @Design(node = "#media/error/auto-content") showErrorFrame: Boolean,
    @Design(node = "#media/error/auto-content") errorFrameContents: @Composable () -> Unit,

    // Browse
    @Design(node = "#media/browse/source-list/auto-content")
    browseSourceList: @Composable () -> Unit,
    @Design(node = "#media/browse/auto-content") browseFrame: @Composable () -> Unit,
    @Design(node = "#media/browse/auto-content") showBrowse: Boolean,
    @Design(node = "#nav/auto-content") nav: @Composable () -> Unit,

    // Up next queue
    @Design(node = "#media/up-next/title") upNextTitle: String,
    @Design(node = "#media/up-next/auto-content") upNextList: @Composable () -> Unit,
    @Design(node = "#media/up-next-button") showUpNext: Boolean,

    // All settings/search buttons in the document
    @Design(node = "#media/search-button") showSearch: Boolean,
    @Design(node = "#media/settings-button") showSettings: Boolean,
    @Design(node = "#media/settings-button") onTapSettings: Modifier,

    // Search overlay
    @Design(node = "#media/search/text-edit")
    searchField: @Composable (ComponentReplacementContext) -> Unit,
    @Design(node = "#media/search/help-text") showSearchHelp: Boolean,
    @Design(node = "#media/search/clear-button") onClearSearch: Modifier,
    @Design(node = "#media/search/clear-button") showClearSearch: Boolean,
    @Design(node = "#media/search/auto-content") searchResultsFrame: @Composable () -> Unit,

    // Car properties
    @Design(node = "#home/properties/content") carPropertiesContent: @Composable () -> Unit,

    // App launchers
    @Design(node = "#home/launchers/content") appLaunchersContent: @Composable () -> Unit,

    // Google assistant
    @Design(node = "#home/google/assistant/content") googleAssistantContent: @Composable () -> Unit,

    // Google maps
    @Design(node = "#home/maps") mapActivity: @Composable () -> Unit,

    // Dimmer frame
    @Design(node = "#home/maps/dimmerframe") dimmerFrame: @Composable () -> Unit,
  )
  @DesignComponent(node = "#media/now-playing/custom-action-button", override = true)
  fun CustomActionButton(
    @Design(node = "#icon") icon: @Composable (ImageReplacementContext) -> Bitmap?,
    @Design(node = "#media/now-playing/custom-action-button") onTap: Modifier
  )
  @DesignComponent(node = "#media/browse/page", override = true)
  fun BrowsePage(
    @DesignVariant(property = "#media/page-header") pageHeaderType: PageHeaderType,
    @Design(node = "#media/page-header") showHeader: Boolean,
    @Design(node = "#title") title: String,
    @Design(node = "#back") onTapBack: Modifier,
    @Design(node = "#auto-content") browseContent: @Composable () -> Unit,
    @Design(node = "#media/browse/page") show: Boolean
  )
  @DesignComponent(node = "#media/browse/loading", override = true) fun LoadingPage()
  @DesignComponent(node = "#media/browse/section-title", override = true)
  fun GroupHeader(@Design(node = "#title") title: String)
  @DesignComponent(node = "#media/source-button", override = true)
  fun SourceButton(
    @DesignVariant(property = "#media/source-button") sourceButtonType: SourceButtonType,
    @Design(node = "#title") title: String,
    @Design(node = "#icon") icon: Bitmap?,
    @Design(node = "#num-results-container") showResults: Boolean,
    @Design(node = "#num-results") numResults: String,
  )
  @DesignComponent(node = "#media/page-header/nav-button", override = true)
  fun PageHeaderNavButton(
    @DesignVariant(property = "#media/page-header/nav-button") navButtonType: NavButtonType,
    @Design(node = "#name") name: String,
    @Design(node = "#icon") icon: @Composable (ImageReplacementContext) -> Bitmap?,
  )
  @DesignComponent(node = "#media/browse/item", override = true)
  fun BrowseItem(
    @DesignVariant(property = "#media/browse/item") browseType: BrowseItemType,
    @DesignVariant(property = "#media/currently-playing") currentlyPlaying: CurrentlyPlaying,
    @Design(node = "#title") title: String,
    @Design(node = "#subtitle") subtitle: String,
    @Design(node = "#subtitle") showSubtitle: Boolean,
    @Design(node = "#icon") icon: @Composable (ImageReplacementContext) -> Bitmap?
  )
  @DesignComponent(node = "#media/error/frame", override = true)
  fun ErrorFrame(
    @Design(node = "#media/error/message") errorMessage: String,
    @Design(node = "#media/error/button-text") errorButtonText: String,
    @Design(node = "#media/error/button") showErrorButton: Boolean,
    @Design(node = "#media/error/button") onTapErrorButton: Modifier
  )
}

class MainActivity : ComponentActivity() {
  private var viewModelProvider: ViewModelProvider? = null
  private lateinit var activityView: EmbeddedActivityView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    DesignSettings.enableLiveUpdates(this)
    DesignSettings.addFontFamily("Inter", interFont)

    viewModelProvider =
      ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application))

    activityView = EmbeddedActivityView(this, this)
    activityView.setLaunchIntent(
      Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS)
    )
    setContent { MainFrame() }
  }

  override fun onResume() {
    super.onResume()
    ImmersiveMode.enterImmersiveMode(this)
  }

  override fun onPause() {
    super.onPause()
    ImmersiveMode.exitImmersiveMode(this)
  }

  // We have to tell the EmbeddedActivityView about these lifecycle events, which aren't
  // observable using the lifecycle observer.
  override fun onRestart() {
    super.onRestart()
    activityView.onRestart()
  }

  @Composable
  fun MainFrame() {
    val carProperties = remember {
      try {
        CarProperties(this)
      } catch (t: Throwable) {
        Log.e("DesignCompose", "Unable to fetch car properties; will show placeholder values...", t)
        null
      }
    }
    val appLauncher = remember {
      val hiddenApps = resources.getStringArray(R.array.hidden_apps).toSet()
      AppLauncher(this, hiddenApps)
    }
    val googleAssistant = remember { GoogleAssistantSuggestions(this, this) }

    val mediaAdapter = remember {
      MediaAdapter(application, applicationContext, viewModelProvider!!, this)
    }
    val nowPlaying = mediaAdapter.getNowPlaying(CenterDisplayDoc)
    val mediaSource = mediaAdapter.getMediaSource()
    val browse = mediaAdapter.getBrowse(CenterDisplayDoc)
    val search = mediaAdapter.getSearch(CenterDisplayDoc)
    val progress = mediaAdapter.getNowPlayingProgress()

    // The car properties frame is also separate so that only it needs to recompose if any car
    // properties change
    val carPropsFrame =
      @Composable {
        val properties = carProperties?.getProperties()
        CenterDisplayDoc.CarPropertiesFrame(
          range = properties?.range ?: "--",
          rangeUnits = "km",
          charge = properties?.charge ?: "--",
          chargeUnits = "%",
          odometer = properties?.odometer ?: "--",
          odometerUnits = "km"
        )
      }

    // Create the launcher list (which listens for package changes).
    val appLaunchersFrame =
      @Composable {
        val appLaunchers = appLauncher.getAppLaunchers()
        CenterDisplayDoc.AppLaunchersFrame(
          content = {
            appLaunchers.forEach {
              CenterDisplayDoc.LauncherIcon(title = it.title, icon = it.icon, onTap = it.onTap)
            }
          }
        )
      }

    // Create the google assistant content
    val googleAssistantFrame =
      @Composable {
        val suggestions = googleAssistant.getSuggestions()
        val shortcuts = googleAssistant.getShortcuts()
        CenterDisplayDoc.GoogleAssistantFrame(
          suggestions = {
            suggestions.forEach {
              CenterDisplayDoc.GoogleAssistantSuggestion(
                title = it.title,
                icon = it.icon,
                onTap = it.onTap
              )
            }
          },
          shortcuts = {
            shortcuts.forEach {
              CenterDisplayDoc.GoogleAssistantShortcut(
                title = it.title,
                icon = it.icon,
                onTap = it.onTap
              )
            }
          }
        )
      }

    val androidMapsView =
      @Composable {
        AndroidView(
          modifier = Modifier.fillMaxSize(),
          factory = { _ -> activityView },
          update = { _ -> }
        )
      }

    val dimmerFrame =
      @Composable {
        CenterDisplayDoc.MapsDimmer(
          modifier =
            Modifier.onGloballyPositioned { coordinates ->
              val bounds = coordinates.boundsInWindow()
              activityView.setExcludeRegion(
                bounds.left.toInt(),
                bounds.top.toInt(),
                bounds.right.toInt(),
                bounds.bottom.toInt()
              )
            },
        )
      }

    CenterDisplayDoc.MainFrame(
      modifier = Modifier.fillMaxSize().background(Color.Black),
      title = nowPlaying.title,
      artist = nowPlaying.artist,
      albumArt = nowPlaying.albumArt,
      album = nowPlaying.album,
      appIcon = mediaSource?.croppedPackageIcon,
      appName = mediaSource?.displayName as String,
      timeElapsed = progress.currentTimeText,
      timeDuration = progress.maxTimeText,
      progress = { context ->
        val progress = mediaAdapter.getNowPlayingProgress()
        val progressWidth =
          if (progress.progressWidth.isFinite()) {
            progress.progressWidth
          } else {
            0.0f
          }
        Row(Modifier.fillMaxHeight().fillMaxWidth(progressWidth)) { context.Content() }
      },
      playState = if (nowPlaying.showPlay) PlayState.Play else PlayState.Pause,
      onPlayPauseTap =
        Modifier.clickable {
          if (nowPlaying.showPlay) nowPlaying.playController?.play()
          else nowPlaying.playController?.pause()
        },
      onPrevTap = Modifier.clickable { nowPlaying.playController?.skipToPrevious() },
      onNextTap = Modifier.clickable { nowPlaying.playController?.skipToNext() },
      showPrev = nowPlaying.showPrev,
      showNext = nowPlaying.showNext,
      customActions = nowPlaying.customActions,
      upNextTitle = nowPlaying.upNextTitle,
      upNextList = nowPlaying.upNextList,
      showUpNext = nowPlaying.showUpNext,
      showErrorFrame = nowPlaying.hasError,
      errorFrameContents = nowPlaying.errorFrame,
      browseSourceList = browse.sourceButtons,
      browseFrame = browse.browseFrame,
      showBrowse = !nowPlaying.hasError,
      nav = browse.nav,
      showSearch = browse.supportsSearch,
      showSettings = browse.showSettings,
      onTapSettings = browse.onTapSettings,
      searchField = search.searchField,
      showSearchHelp = search.query.isEmpty(),
      onClearSearch =
        Modifier.clickable {
          search.searchFunc("")
          search.setQuery("")
        },
      showClearSearch = search.query.isNotEmpty(),
      searchResultsFrame = search.browseFrame,
      carPropertiesContent = { carPropsFrame() },
      appLaunchersContent = { appLaunchersFrame() },
      googleAssistantContent = { googleAssistantFrame() },
      mapActivity = { androidMapsView() },
      dimmerFrame = { dimmerFrame() }
    )
  }
}

@Preview(widthDp = 500, showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
fun CarPropsPreview() {
  CenterDisplayDoc.CarPropertiesFrame(
    range = "300",
    rangeUnits = "km",
    charge = "90",
    chargeUnits = "%",
    odometer = "8675309",
    odometerUnits = "km"
  )
}
