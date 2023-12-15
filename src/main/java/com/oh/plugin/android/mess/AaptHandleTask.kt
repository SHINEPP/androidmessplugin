package com.oh.plugin.android.mess

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class AaptHandleTask : DefaultTask() {

    companion object {
        private val taskNames = arrayOf("mess_", "_aapt_rules")

        fun getTaskName(variantName: String): String {
            return taskNames.toMutableList().also {
                it.add(1, variantName.lowercase())
            }.joinToString("")
        }
    }

    @get:OutputFile
    abstract val outputAaptProguard: RegularFileProperty

    @TaskAction
    fun taskAction() {
        println("MessTask::taskAction: ${outputAaptProguard.get()}")
        outputAaptProguard.get().asFile.writeText("")
    }
}