// Copyright 2023 Google LLC

package com.android.designcompose.testapp.clusterdemo

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

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
val notoSansFontFamily =
    FontFamily(
        Font(R.font.notosans_black, FontWeight.Black),
        Font(R.font.notosans_light, FontWeight.Light),
        Font(R.font.notosans_thin, FontWeight.Thin),
        Font(R.font.notosans_bold, FontWeight.Bold),
    )

val ptSansNarrowFontFamily =
    FontFamily(
        Font(R.font.ptsansnarrow_bold, FontWeight.Bold),
        Font(R.font.ptsansnarrow_regular, FontWeight.Normal),
    )

val robotoFontFamily = interFont // Wrong in every way.
