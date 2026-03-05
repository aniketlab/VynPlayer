package com.muzic.player.util

/**
 * Cleans up song metadata by removing common spam tags from download sites.
 */
object MetadataUtils {

    private val spamPatterns = listOf(
        "pagalworld", "pagalnew", "djpunjab", "mr-jat", "mrjat", "mr-?jatt", "320kbps",
        "download", "mp3 songs", "djmaza", "songspk", "djjohal", "djyoungster",
        "downloadming", "mp3mad", "freshmaza", "songslover", "pendujatt", 
        "djlemon", "raagfm", "bestwap", "funmaza", "wapking", "mp3skull", 
        "riskyjatt", "mp3hungama", "webmusic", "raag.fm", "hd"
    )

    private val domainPatterns = listOf(
        ".com", ".in", ".net", ".org", ".co", ".io", ".me", ".cc"
    )

    private val bracketRegex = Regex("""\[.*?]|\(.*?\)""")
    private val numberingRegex = Regex("""^(?:\d+[\s\-–—|._]+|track\s*\d+[\s\-–—|._]*)+""", RegexOption.IGNORE_CASE)

    fun cleanTitle(title: String): String {
        var cleaned = title

        // Remove numbering like "01-", "01_ ", "03. ", "1 " 
        cleaned = numberingRegex.replace(cleaned, "").trim()

        // Remove bracketed content that contains spam
        cleaned = bracketRegex.replace(cleaned) { match ->
            val content = match.value.lowercase()
            if (spamPatterns.any { content.contains(it) } ||
                domainPatterns.any { content.contains(it) }) {
                ""
            } else {
                match.value
            }
        }

        // Remove any remaining spam words (case insensitive)
        for (pattern in spamPatterns) {
            cleaned = cleaned.replace(Regex("(?i)\\s*[-–—|]?\\s*$pattern\\s*"), " ")
        }

        // Remove domain-like suffixes
        for (domain in domainPatterns) {
            cleaned = cleaned.replace(Regex("(?i)\\S*${Regex.escape(domain)}\\S*"), "")
        }

        // Clean up whitespace
        cleaned = cleaned.trim().replace(Regex("\\s{2,}"), " ")
        // Remove trailing dashes/pipes
        cleaned = cleaned.replace(Regex("[\\-–—|]+\\s*$"), "").trim()
        // Remove leading dashes/pipes
        cleaned = cleaned.replace(Regex("^\\s*[\\-–—|]+"), "").trim()

        return toTitleCase(cleaned.ifBlank { title })
    }

    fun cleanArtist(artist: String): String {
        var cleaned = artist

        // Remove spam patterns
        for (pattern in spamPatterns) {
            cleaned = cleaned.replace(Regex("(?i)\\s*[-–—|,]?\\s*$pattern\\s*"), " ")
        }

        // Remove domain-like names
        for (domain in domainPatterns) {
            cleaned = cleaned.replace(Regex("(?i)\\S*${Regex.escape(domain)}\\S*"), "")
        }

        // Remove "www." prefixed strings
        cleaned = cleaned.replace(Regex("(?i)www\\.\\S+"), "")

        // Remove "http" prefixed strings
        cleaned = cleaned.replace(Regex("(?i)https?://\\S+"), "")

        // Clean up whitespace and separators
        cleaned = cleaned.trim().replace(Regex("\\s{2,}"), " ")
        cleaned = cleaned.replace(Regex("[\\-–—|,]+\\s*$"), "").trim()
        cleaned = cleaned.replace(Regex("^\\s*[\\-–—|,]+"), "").trim()

        return toTitleCase(cleaned.ifBlank { artist })
    }

    /**
     * Tries to extract Title and Artist from a filename if they are missing in tags.
     * Expected pattern: "Title - Artist" or "Artist - Title" or "Title-Artist"
     */
    fun extractFromFileName(fileName: String): Pair<String?, String?> {
        val name = fileName.substringBeforeLast(".") // Remove extension
        val separators = listOf(" - ", " – ", " — ", " | ")
        
        for (sep in separators) {
            if (name.contains(sep)) {
                val parts = name.split(sep)
                if (parts.size >= 2) {
                    val p1 = parts[0].trim()
                    val p2 = parts[1].trim()
                    
                    // Simple logic: if p1 is just a number, it's probably Title numbering, not Artist.
                    if (p1.matches(Regex("""^\d+$"""))) {
                        return Pair(p2, null) 
                    }
                    
                    return Pair(p2, p1) // Title usually comes after "Artist - " or before " - Artist"
                }
            }
        }
        return Pair(null, null)
    }

    /**
     * Final sanitization to ensure no "0" or "unknown" strings make it to the UI.
     */
    fun sanitize(text: String?, type: MetadataType): String {
        val unknown = when (type) {
            MetadataType.TITLE -> "Unknown Title"
            MetadataType.ARTIST -> "Unknown Artist"
            MetadataType.ALBUM -> "Unknown Album"
        }

        if (text == null || text.isBlank() || text == "0" || text.lowercase().contains("unknown")) {
            return unknown
        }

        // Check if text contains any forbidden junk words
        val lowerText = text.lowercase()
        if (spamPatterns.any { lowerText.contains(it) }) {
            return unknown
        }

        return text
    }

    fun toTitleCase(text: String): String {
        if (text.isBlank()) return text
        return text.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    fun containsJunk(text: String): Boolean {
        val lower = text.lowercase()
        return spamPatterns.any { lower.contains(it) }
    }

    enum class MetadataType { TITLE, ARTIST, ALBUM }
}
