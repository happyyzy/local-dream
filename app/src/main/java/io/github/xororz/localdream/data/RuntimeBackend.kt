package io.github.xororz.localdream.data

enum class RuntimeBackend(val value: String) {
    CPU("cpu"),
    OPENCL("opencl"),
    ADRENO("adreno");

    companion object {
        fun fromValue(value: String?): RuntimeBackend {
            return values().firstOrNull { it.value == value } ?: CPU
        }

        fun fromLegacy(useOpenCL: Boolean): RuntimeBackend {
            return if (useOpenCL) OPENCL else CPU
        }
    }
}
