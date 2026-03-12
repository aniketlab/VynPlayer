package com.vyn.player.util

import java.util.regex.Pattern

/**
 * Utility to clean song titles and artist names of common junk text
 * found in downloaded tracks (e.g. "www.djpunjab.com", "(Dj Remix)", etc.)
 */
object MetadataSanitizer {
    
    // Patterns to remove (case-insensitive)
    private val junkPatterns = listOf(
        // Website patterns
        "www\\.[^\\s]+\\.[a-z]{2,4}",
        "(http|https)://[^\\s]+",
        "\\.com", "\\.in", "\\.net", "\\.org", "\\.me", "\\.info",
        
        // Site names
        "pagalworld", "djpunjab", "djmaza", "mr-jatt", "songspk", "onewap", "freshmaza", "wapking", "songszilla", "mp3download",
        
        // Quality/Metadata
        "320kbps", "128kbps", "190kbps", "vbr", "kbps", "mp3", "download",
        
        // Generic junk
        "\\(dj remix\\)", "\\[dj remix\\]", "dj remix", "\\(official\\)", "\\[official\\]",
        "full video", "video song", "official video", "lyric video",
        "audio song", "exclusive", "latest",
        
        // Track numbers at start (e.g. "01 - Title", "02. Title")
        "^\\d{1,2}\\s*[-.]*\\s*"
    ).map { Pattern.compile(it, Pattern.CASE_INSENSITIVE) }

    /**
     * Sanitizes a string by removing common junk patterns and cleaning up whitespace.
     */
    fun sanitize(input: String?): String {
        if (input == null) return ""
        
        var cleaned = input
        
        // Remove known junk patterns
        for (pattern in junkPatterns) {
            cleaned = pattern.matcher(cleaned as CharSequence).replaceAll("")
        }
        
        // Remove any remaining content in parentheses/brackets that might be junk
        // E.g., "(pagalworld.com)" might become "()" after above generic replacements if we aren't careful.
        // Or if the user doesn't want ANY parentheses since they're usually remixes: 
        cleaned = cleaned.replace(Regex("\\(.*?\\)"), "")
        cleaned = cleaned.replace(Regex("\\[.*?\\]"), "")
        
        // Remove duplicate separators (like " - - " or " & & ")
        cleaned = cleaned.replace(Regex("(\\s*[-&,]\\s*){2,}"), " - ")
        
        // Clean up whitespace
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()
        
        // If string ends with a dash, strip it
        if (cleaned.endsWith("-")) cleaned = cleaned.substring(0, cleaned.length - 1).trim()
        
        // If everything was stripped, return original to be safe
        return if (cleaned.isBlank()) input.trim() else cleaned
    }
}
