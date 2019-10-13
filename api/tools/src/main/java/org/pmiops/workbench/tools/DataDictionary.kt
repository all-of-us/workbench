package org.pmiops.workbench.tools

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class DataDictionary(val meta_data: Array<MetaData>, val transformations: Array<Transformations>)