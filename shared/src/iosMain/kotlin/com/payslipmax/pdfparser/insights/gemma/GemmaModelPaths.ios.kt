@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.payslipmax.pdfparser.insights.gemma

import com.payslipmax.pdfparser.subscription.isDebugBuild
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun gemmaModelStorageDir(): String = documentDirectory()

actual fun fileExistsAt(path: String): Boolean = path.isNotEmpty() && NSFileManager.defaultManager.fileExistsAtPath(path)

/**
 * Resource name (sans extension) the model is tagged under in the Xcode ODR asset catalog — see
 * `GEMMA_ODR_TAG` in `GemmaOnDemandResourceBridge.swift`. Must match the file's on-disk base name
 * exactly, since [NSBundle.pathForResource] looks it up by name+extension, not by ODR tag.
 */
private const val GEMMA_RESOURCE_NAME = "gemma-active"
private const val GEMMA_RESOURCE_EXTENSION = "litertlm"

/**
 * Resolves the Gemma base model's on-disk path once Apple's On-Demand Resources (ODR) mechanism has
 * fetched it. Once an ODR-tagged resource has been downloaded and a live `NSBundleResourceRequest`
 * holds access to it (see `GemmaOnDemandResourceBridge.swift`), it becomes queryable through the
 * ordinary [NSBundle.mainBundle] resource lookup — no App Group or shared container needed, since
 * (unlike Background Assets) ODR resources land inside the main app bundle's own on-demand storage.
 * Returns null if the resource hasn't been fetched yet (or has been purged by the OS for storage
 * pressure), which the caller already treats as "not installed yet".
 *
 * Debug builds additionally fall back to a manually-sideloaded file in the app's Documents
 * directory (e.g. dropped in via Xcode's device file browser) — useful for testing Tier 6 in the
 * simulator, where ODR fetches don't reliably trigger. Release builds never take this branch.
 */
actual fun resolveInstalledGemmaModelPath(): String? {
    val odrPath = NSBundle.mainBundle.pathForResource(GEMMA_RESOURCE_NAME, ofType = GEMMA_RESOURCE_EXTENSION)
    if (odrPath != null && fileExistsAt(odrPath)) return odrPath
    if (isDebugBuild()) {
        val fileName = GemmaModelStorageManager().getRecommendedModelFileName()
        val sideloadPath = "${gemmaModelStorageDir()}/$fileName"
        if (fileExistsAt(sideloadPath)) return sideloadPath
    }
    return null
}

private fun documentDirectory(): String {
    val documentDirectory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
    return documentDirectory?.path ?: ""
}
