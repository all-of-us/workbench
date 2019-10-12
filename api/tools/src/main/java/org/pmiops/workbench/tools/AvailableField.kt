package org.pmiops.workbench.tools

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AvailableField(val field_name: String, val relevant_omop_table: String)