package com.agentwallet.util

object MarkdownUtil {
    fun stripMarkdown(text: String): String {
        return text
            .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`{1,3}[^`]*`{1,3}"), "")
            .replace(Regex("^-{3,}\\s*$", RegexOption.MULTILINE), "")
            .replace(Regex("\\[([^]]*)]\\([^)]*\\)"), "$1")
            .replace(Regex("\\n---\\n"), "\n")
            .trim()
    }
}
