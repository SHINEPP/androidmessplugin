package com.oh.plugin.android.mess

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.configurationcache.extensions.capitalized
import proguard.obfuscate.MappingProcessor
import proguard.obfuscate.MappingReader
import java.io.InputStream
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

abstract class AarHandleTask : DefaultTask() {

    companion object {
        private val taskNames = arrayOf("mess", "HandleAar")

        private const val MANIFEST_NAME = "AndroidManifest.xml"

        fun getTaskName(variantName: String): String {
            return taskNames.toMutableList().also {
                it.add(1, variantName.capitalized())
            }.joinToString("")
        }
    }

    @get:InputFile
    @get:Optional
    abstract val mappingFile: RegularFileProperty

    @get:InputFile
    abstract val inputAar: RegularFileProperty

    @get:OutputFile
    abstract val outputAar: RegularFileProperty

    @TaskAction
    fun taskAction() {
        println("ManifestTask::taskAction: inputAar = ${inputAar.get().asFile}")
        println("ManifestTask::taskAction: outputAar = ${outputAar.get().asFile}")

        outputAar.get().asFile.deleteOnExit()
        if (!mappingFile.isPresent) {
            inputAar.get().asFile.copyTo(outputAar.get().asFile)
            return
        }

        println("ManifestTask::taskAction: mappingFile = ${mappingFile.get().asFile}")
        val classMap = parseMappingClassMap()
        ZipFile(inputAar.get().asFile).use { zipFile ->
            outputAar.get().asFile.parentFile.mkdirs()
            ZipOutputStream(outputAar.get().asFile.outputStream()).use { zipOutputStream ->
                val entries = zipFile.entries()
                for (entry in entries) {
                    val name = entry.name
                    zipOutputStream.putNextEntry(ZipEntry(name))
                    val inputStream = zipFile.getInputStream(entry)
                    if (name == MANIFEST_NAME) {
                        zipOutputStream.write(handleManifest(classMap, inputStream))
                    } else {
                        inputStream.copyTo(zipOutputStream)
                    }
                }
            }
        }
    }

    private fun parseMappingClassMap(): Map<String, String> {
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
        return classMap
    }

    private fun handleManifest(classMap: Map<String, String>, inputStream: InputStream): ByteArray {
        val newLines = ArrayList<String>()
        val pattern = Pattern.compile(".*android:name=\"(.*)\".*")
        val markPattern = Pattern.compile("<!--.*-->")
        val lines = inputStream.bufferedReader().readLines()
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
        return newLines.joinToString("\n").toByteArray()
    }
}