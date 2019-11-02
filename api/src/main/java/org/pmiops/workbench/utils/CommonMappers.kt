package org.pmiops.workbench.utils

import java.sql.Timestamp
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
object CommonMappers {

    fun timestamp(timestamp: Timestamp?): Long? {
        return timestamp?.time

    }

    fun timestamp(timestamp: Long?): Timestamp? {
        return if (timestamp != null) {
            Timestamp(timestamp)
        } else null

    }
}
