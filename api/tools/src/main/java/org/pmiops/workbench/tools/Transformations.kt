package org.pmiops.workbench.tools

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Transformations(val available_fields: Array<AvailableField>?)