package com.example.validation

object AndroidNamingValidator {

    private val URL_REGEX = "^(https?:\\/\\/)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(:\\d+)?(\\/.*)?$".toRegex()
    private val PACKAGE_NAME_REGEX = "^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+$".toRegex()

    private val JAVA_KEYWORDS = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
        "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
        "true", "false", "null", "val", "var", "fun", "object", "typealias", "when", "as", "is", "in", "out"
    )

    fun validateUrl(url: String): Boolean {
        return url.isNotBlank() && URL_REGEX.matches(url)
    }

    fun validatePackageName(packageName: String): Boolean {
        if (packageName.isBlank() || !PACKAGE_NAME_REGEX.matches(packageName)) {
            return false
        }
        val segments = packageName.split(".")
        for (segment in segments) {
            if (JAVA_KEYWORDS.contains(segment.lowercase())) {
                return false
            }
        }
        return true
    }

    fun getUrlValidationError(url: String): String? {
        if (url.isBlank()) {
            return "Website URL is required and cannot be empty."
        }
        if (!URL_REGEX.matches(url)) {
            return "Invalid website URL format (must be a valid domain structure e.g. 'https://example.com' or 'example.com')."
        }
        return null
    }

    fun getPackageNameValidationError(packageName: String): String? {
        if (packageName.isBlank()) {
            return "Package name is required and cannot be empty."
        }
        if (!PACKAGE_NAME_REGEX.matches(packageName)) {
            return "Invalid structure. Must contain at least two segments separated by a dot, begin with letters/underscores, and contain no special characters (e.g. 'com.example')."
        }
        val segments = packageName.split(".")
        for (segment in segments) {
            if (JAVA_KEYWORDS.contains(segment.lowercase())) {
                return "Segment '$segment' is a reserved Java/Kotlin keyword and cannot be used in a package name."
            }
        }
        return null
    }
}
