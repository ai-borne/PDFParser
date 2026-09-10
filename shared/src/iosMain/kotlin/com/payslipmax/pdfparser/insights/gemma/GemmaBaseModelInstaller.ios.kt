package com.payslipmax.pdfparser.insights.gemma

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * On-Demand Resources (ODR)-backed installer for the Tier 6 base model. Unlike Background Assets,
 * ODR is app-triggered like Android's Play Asset Delivery: [install] calls [installTrigger], which
 * `GemmaOnDemandResourceBridge.swift` wires to `NSBundleResourceRequest.beginAccessingResources`.
 *
 * Progress/completion arrive via [progressReporter]/[completionReporter]: this instance registers
 * its own reactive closures into those companion-object slots at construction, and
 * `GemmaOnDemandResourceBridge.swift` — registered once from `iOSApp.swift`'s
 * `AppDelegate.didFinishLaunchingWithOptions`, the same call-site pattern as
 * `GemmaInferenceBridge.register()` — forwards the real ODR progress/completion events into them.
 */
class IosGemmaBaseModelInstaller : GemmaBaseModelInstaller {
    private val _state = MutableStateFlow<BaseModelInstallState>(BaseModelInstallState.NotStarted)
    override val state: StateFlow<BaseModelInstallState> = _state.asStateFlow()

    init {
        progressReporter = { bytesDownloaded, totalBytes ->
            _state.value =
                BaseModelInstallState.Downloading(
                    if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes.toFloat() else 0f,
                )
        }
        completionReporter = { success, errorMessage ->
            _state.value =
                if (success) {
                    BaseModelInstallState.Installed(resolveInstalledGemmaModelPath() ?: "")
                } else {
                    BaseModelInstallState.Failed(errorMessage ?: "On-Demand Resource download failed")
                }
        }
    }

    override suspend fun install() {
        // Reflect an already-fetched resource immediately (e.g. a re-verify-on-init call after a
        // previous launch completed the fetch) rather than re-triggering it needlessly.
        resolveInstalledGemmaModelPath()?.let { path ->
            _state.value = BaseModelInstallState.Installed(path)
            return
        }
        installTrigger?.invoke()
    }

    companion object {
        /** Invoked to start the ODR fetch; set by GemmaOnDemandResourceBridge.swift at registration. */
        var installTrigger: (() -> Unit)? = null

        /** Invoked by GemmaOnDemandResourceBridge.swift with (bytesDownloaded, totalBytes). */
        var progressReporter: ((Long, Long) -> Unit)? = null

        /** Invoked by GemmaOnDemandResourceBridge.swift with (success, errorMessage). */
        var completionReporter: ((Boolean, String?) -> Unit)? = null
    }
}

actual fun provideGemmaBaseModelInstaller(): GemmaBaseModelInstaller = IosGemmaBaseModelInstaller()
