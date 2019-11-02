package org.pmiops.workbench.tools

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.sql.Timestamp
import java.text.ParseException
import java.text.SimpleDateFormat

/** Parses and formats SQL timestamps when using GSON.  */
class TimestampGsonAdapter : TypeAdapter<Timestamp>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Timestamp?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(TIME_FORMAT.format(value))
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader?): Timestamp? {
        return if (`in` != null) {
            try {
                Timestamp(TIME_FORMAT.parse(`in`.nextString()).time)
            } catch (e: ParseException) {
                throw IOException(e)
            }

        } else {
            null
        }
    }

    companion object {

        private val TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'")
    }
}
