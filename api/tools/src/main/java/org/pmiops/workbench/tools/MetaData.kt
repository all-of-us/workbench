package org.pmiops.workbench.tools

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.sql.Timestamp

@JsonIgnoreProperties(ignoreUnknown = true)
data class MetaData(
        val id: String,
        val name: String,
        val version: Long,
        val created_time: Timestamp,
        val modified_time: Timestamp,
        val last_modifying_user_display_name: String,
        val cdr_version: String
)