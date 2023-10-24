package com.android.designcompose.cargoplugin

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecSpec
import java.io.File
import javax.inject.Inject

enum class CargoBuildType {
    RELEASE,
    DEBUG;

    override fun toString() = if (this == RELEASE) "release" else "debug"
}

fun String.toCargoBuildType() =
    if (this.compareTo("release", true) == 0) CargoBuildType.RELEASE
    else if (this.compareTo("debug", true) == 0) CargoBuildType.DEBUG else null

@UntrackedTask(
    because =
        "Cargo has it's own up-to-date checks. Trying to reproduce them so that we don't need to run Cargo is infeasible, and any errors will cause out-of-date code to be included"
)
abstract class CargoBuildBaseTask : DefaultTask() {
    @get:Inject abstract val fs: FileSystemOperations

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val rustSrcs: ConfigurableFileCollection

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFile
    abstract val cargoBin: Property<File>

    @get:Input abstract val hostOS: Property<String>

    @get:Input abstract val buildType: Property<CargoBuildType>

    @get:OutputDirectory abstract val outLibDir: DirectoryProperty

//    @get:OutputFile abstract val outputFile: RegularFileProperty

    @get:Internal abstract val cargoTargetDir: DirectoryProperty

    @Internal
    fun applyCommonCargoConfig(
        cargoExtension: CargoPluginExtension,
        project: Project,
        theBuildType: CargoBuildType
    ) {
        // Set the cargoBinary location from the configured plugin extension, or default to
        // the standard install location
        cargoBin.set(
            cargoExtension.cargoBin.orElse(
                project.providers.systemProperty("user.home").map { File(it, ".cargo/bin/cargo") }
            )
        )
        rustSrcs.setFrom(cargoExtension.crateDir.filter { !it.asFile.path.startsWith("target") })

        hostOS.set(
            if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                if (Os.isArch("x86_64") || Os.isArch("amd64")) {
                    "windows-x86_64"
                } else {
                    "windows"
                }
            } else if (Os.isFamily(Os.FAMILY_MAC)) {
                "darwin-x86_64"
            } else {
                "linux-x86_64"
            }
        )

        buildType.set(theBuildType)

        cargoTargetDir.set(
            project.layout.buildDirectory.map { it.dir("intermediates/cargoTarget") }
        )
//        outputFile.set(outLibDir.flatMap { project.provider { it.file("libjni.so") } })

        group = "build"
        // Try to get the cargo build started earlier in the build execution.
//        shouldRunAfter(project.tasks.named("preBuild"))
    }

    fun baseExecOptions(
        it: ExecSpec,
    ) {
        println("OutLibDir: ${outLibDir.get()}")
        it.executable(cargoBin.get().absolutePath)
        it.workingDir(rustSrcs.asPath)

        it.args("build")
        it.args("--target-dir=${cargoTargetDir.get().asFile.absolutePath}")
        it.args("--quiet")
        if (buildType.get() == CargoBuildType.RELEASE) it.args("--release")
    }
}
