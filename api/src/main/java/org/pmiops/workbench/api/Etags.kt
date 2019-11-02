package org.pmiops.workbench.api

import java.util.regex.Matcher
import java.util.regex.Pattern
import org.pmiops.workbench.exceptions.BadRequestException

/**
 * Utility class for creating API etags, to prevent versioning issues during read-modify-write
 * cycles for API clients.
 */
object Etags {
    private val ETAG_FORMAT = "\"%d\""
    private val ETAG_PATTERN = Pattern.compile("^\"(\\d+)\"$")

    fun fromVersion(version: Int): String {
        return String.format(ETAG_FORMAT, version)
    }

    fun toVersion(etag: String): Int {
        val m = ETAG_PATTERN.matcher(etag)
        if (!m.matches()) {
            throw BadRequestException(String.format("Invalid etag provided: %s", etag))
        }
        return Integer.parseInt(m.group(1))
    }
}
