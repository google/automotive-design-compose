package com.android.designcompose.cargoplugin


// If true, allow the abi list to be overriden by Android Studio (first) and by the overrideAbi property (second)
const val allowAbiOverride = "designcompose.cargoPlugin.allowAbiOverride"
// Comma separated list of abis to build instead of the configured ones. Must be a subset of the configured ABIs.
const val abiFilter = "designcompose.cargoPlugin.abiFilter"
