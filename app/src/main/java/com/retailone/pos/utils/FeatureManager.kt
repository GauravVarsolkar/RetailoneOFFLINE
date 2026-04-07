package com.retailone.pos.utils

object FeatureManager {

    private var enabledModules: Set<String> = emptySet()

    fun init(modules: List<String>) {
        enabledModules = modules.map { it.lowercase().trim() }.toSet()
    }

    fun isEnabled(feature: String): Boolean {
        return try {
            enabledModules.contains(feature.lowercase().trim())
        } catch (e: Exception) {
            false
        }
    }

    fun isInitialized(): Boolean = enabledModules.isNotEmpty()
}