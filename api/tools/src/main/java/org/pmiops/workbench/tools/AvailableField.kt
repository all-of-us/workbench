package org.pmiops.workbench.tools

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class AvailableField(
        val field_name: String,
        val relevant_omop_table: String,
        val omop_cdm_standard_or_custom_field: String,
        val description: String,
        val field_type: String,
        val data_provenance: String,
        val source_ppi_module: String?,
        val transformed_by_registered_tier_privacy_methods: Boolean
)
