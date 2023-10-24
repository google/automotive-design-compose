package com.android.designcompose.cargoplugin

enum class CargoBuildType {
    RELEASE,
    DEBUG;

    override fun toString() = if (this == RELEASE) "release" else "debug"
}

fun String.toCargoBuildType() =
    if (this.compareTo("release", true) == 0) CargoBuildType.RELEASE
    else if (this.compareTo("debug", true) == 0) CargoBuildType.DEBUG else null