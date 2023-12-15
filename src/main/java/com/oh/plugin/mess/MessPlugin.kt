package com.oh.plugin.mess

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.internal.scope.InternalArtifactType
import org.gradle.api.Plugin
import org.gradle.api.Project

class MessPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        println("MessPlugin::apply: $project")

        val extensions = project.extensions
        val androidComponents = extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            handleVariant(project, variant)
        }
    }

    private fun handleVariant(project: Project, variant: Variant) {
        // aapt
        val aaptProcessProvider = project.tasks.register(AaptHandleTask.getTaskName(variant.name), AaptHandleTask::class.java)
        variant.artifacts.use(aaptProcessProvider)
            .wiredWith(AaptHandleTask::outputAaptProguard)
            .toCreate(InternalArtifactType.AAPT_PROGUARD_FILE)

        // manifest
        val mappingFile = variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)
        val manifestProcessProvider = project.tasks.register(ManifestHandleTask.getTaskName(variant.name), ManifestHandleTask::class.java) {
            it.mappingFile.set(mappingFile)
        }
        variant.artifacts.use(manifestProcessProvider)
            .wiredWithFiles(ManifestHandleTask::inputManifest, ManifestHandleTask::outputManifest)
            .toTransform(SingleArtifact.MERGED_MANIFEST)
    }
}