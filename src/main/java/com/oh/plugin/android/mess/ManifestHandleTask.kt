package com.oh.plugin.android.mess

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import proguard.obfuscate.MappingProcessor
import proguard.obfuscate.MappingReader
import java.util.regex.Pattern

abstract class ManifestHandleTask : DefaultTask() {

    companion object {
        private val taskNames = arrayOf("mess_", "_manifest")

        fun getTaskName(variantName: String): String {
            return taskNames.toMutableList().also {
                it.add(1, variantName.lowercase())
            }.joinToString("")
        }
    }

    @get:InputFile
    @get:Optional
    abstract val mappingFile: RegularFileProperty

    @get:InputFile
    abstract val inputManifest: RegularFileProperty

    @get:OutputFile
    abstract val outputManifest: RegularFileProperty

    @TaskAction
    fun taskAction() {
        if (!mappingFile.isPresent) {
            inputManifest.get().asFile.copyTo(outputManifest.get().asFile)
            return
        }

        val classMap = LinkedHashMap<String, String>()
        val mappingReader = MappingReader(mappingFile.get().asFile)
        mappingReader.pump(object : MappingProcessor {
            override fun processClassMapping(className: String?, newClassName: String?): Boolean {
                if (className != null && newClassName != null) {
                    classMap[className] = newClassName
                }
                return false
            }

            override fun processFieldMapping(className: String?, fieldType: String?, fieldName: String?, newClassName: String?, newFieldName: String?) {
            }

            override fun processMethodMapping(className: String?,
                                              firstLineNumber: Int,
                                              lastLineNumber: Int,
                                              methodReturnType: String?,
                                              methodName: String?,
                                              methodArguments: String?,
                                              newClassName: String?,
                                              newFirstLineNumber: Int,
                                              newLastLineNumber: Int,
                                              newMethodName: String?) {
            }
        })

        val newLines = ArrayList<String>()
        val pattern = Pattern.compile(".*android:name=\"(.*)\".*")
        val markPattern = Pattern.compile("<!--.*-->")
        val lines = inputManifest.get().asFile.readLines()
        for (line in lines) {
            val trimLine = line.trim()
            if (trimLine.isEmpty() || markPattern.matcher(trimLine).find()) {
                continue
            }

            val matcher = pattern.matcher(trimLine)
            if (!matcher.find()) {
                newLines.add(line)
                continue
            }

            val classPath = matcher.group(1)
            val newClassPath = classMap[classPath]
            if (newClassPath == null) {
                newLines.add(line)
                continue
            }

            newLines.add(line.replace(classPath, newClassPath))
            println("ManifestTask::taskAction: changeManifest, classPath = $classPath, newClassPath = $newClassPath")
        }
        outputManifest.get().asFile.writeText(newLines.joinToString("\n"))
    }
}