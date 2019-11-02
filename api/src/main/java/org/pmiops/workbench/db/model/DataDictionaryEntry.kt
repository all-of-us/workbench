package org.pmiops.workbench.db.model

import java.sql.Timestamp
import java.util.Objects
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "data_dictionary_entry")
class DataDictionaryEntry {

    // Metadata fields
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "data_dictionary_entry_id")
    var dataDictionaryEntryId: Long = 0
    @get:ManyToOne
    @get:JoinColumn(name = "cdr_version_id")
    var cdrVersion: CdrVersion? = null
    @get:Column(name = "defined_time")
    var definedTime: Timestamp? = null

    // Fields copied from the Data Dictionary export
    // See available_field_item in the following
    // https://github.com/all-of-us/cdrdatadictionary/blob/development/cdr_data_dictionary/schema.yaml
    @get:Column(name = "relevant_omop_table")
    var relevantOmopTable: String? = null
    @get:Column(name = "field_name")
    var fieldName: String? = null
    @get:Column(name = "omop_cdm_standard_or_custom_field")
    var omopCdmStandardOrCustomField: String? = null
    @get:Column(name = "description")
    var description: String? = null
    @get:Column(name = "field_type")
    var fieldType: String? = null
    @get:Column(name = "data_provenance")
    var dataProvenance: String? = null
    @get:Column(name = "source_ppi_module")
    var sourcePpiModule: String? = null
    @get:Column(name = "transformed_by_registered_tier_privacy_methods")
    var transformedByRegisteredTierPrivacyMethods: Boolean? = null

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as DataDictionaryEntry?
        return (dataDictionaryEntryId == that!!.dataDictionaryEntryId
                && cdrVersion == that.cdrVersion
                && definedTime == that.definedTime
                && relevantOmopTable == that.relevantOmopTable
                && fieldName == that.fieldName
                && omopCdmStandardOrCustomField == that.omopCdmStandardOrCustomField
                && description == that.description
                && fieldType == that.fieldType
                && dataProvenance == that.dataProvenance
                && sourcePpiModule == that.sourcePpiModule
                && transformedByRegisteredTierPrivacyMethods == that.transformedByRegisteredTierPrivacyMethods)
    }

    override fun hashCode(): Int {
        return Objects.hash(
                dataDictionaryEntryId,
                cdrVersion,
                definedTime,
                relevantOmopTable,
                fieldName,
                omopCdmStandardOrCustomField,
                description,
                fieldType,
                dataProvenance,
                sourcePpiModule,
                transformedByRegisteredTierPrivacyMethods)
    }
}
