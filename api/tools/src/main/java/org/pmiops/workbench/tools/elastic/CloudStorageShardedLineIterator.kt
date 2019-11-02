package org.pmiops.workbench.tools.elastic

import com.google.cloud.storage.Blob
import com.google.common.collect.Lists
import java.io.BufferedReader
import java.io.IOException
import java.nio.channels.Channels
import java.nio.charset.Charset
import java.util.NoSuchElementException
import java.util.Queue

/**
 * An iterator over a set of new-line delimited files in Google Cloud Storage. Intended to be used
 * in combination with BigQuery export, which can write newline delimited JSON or CSV files when
 * dealing with large result sets.
 */
class CloudStorageShardedLineIterator internal constructor(blobs: Iterable<Blob>) : Iterator<String> {

    private val blobs: Queue<Blob>
    private var br: BufferedReader? = null
    // Always buffer the next value so we can easily answer hasNext().
    private var next: String? = null

    init {
        this.blobs = Lists.newLinkedList(blobs)
        this.next = readNext()
    }

    override fun hasNext(): Boolean {
        return next != null
    }

    override fun next(): String? {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        val ret = next
        next = readNext()
        return ret
    }

    private fun readNext(): String? {
        if (br == null) {
            br = openBlob(blobs.remove())
        }
        try {
            var line: String
            while ((line = br!!.readLine()) == null) {
                if (blobs.isEmpty()) {
                    return null
                }
                br = openBlob(blobs.remove())
            }
            return line
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    private fun openBlob(b: Blob): BufferedReader {
        // Call to GCS are slow, so use a big 64MiB buffer to minimize total requests.
        return BufferedReader(
                Channels.newReader(b.reader(), Charset.forName("UTF-8").newDecoder(), 64 shl 20))
    }
}
