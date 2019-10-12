package org.pmiops.workbench.tools

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class DataDictionary(val transformations: Array<Transformations>)