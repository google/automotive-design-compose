package com.android.designcompose.cargoplugin

import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class CargoBuildHostTask  @Inject constructor(private val executor: ExecOperations) :
    CargoBuildBaseTask() {


    @TaskAction fun runCommand() {
        cargoTargetDir.get().asFile.mkdirs()

        executor.exec {
            baseExecOptions(it)
        }

        fs.copy {
            it.from(cargoTargetDir.get().dir(buildType.get().toString()))
            it.include("*.so")
            it.into(outLibDir)
        }
    }
}