package org.pmiops.workbench.test

import java.util.Arrays
import org.pmiops.workbench.model.CriteriaType
import org.pmiops.workbench.model.Domain
import org.pmiops.workbench.model.DomainType
import org.pmiops.workbench.model.SearchGroup
import org.pmiops.workbench.model.SearchGroupItem
import org.pmiops.workbench.model.SearchParameter
import org.pmiops.workbench.model.SearchRequest
import org.pmiops.workbench.model.TemporalMention
import org.pmiops.workbench.model.TemporalTime

object SearchRequests {

    private val MALE_CONCEPT_ID: Long = 8507
    private val FEMALE_CONCEPT_ID: Long = 8532
    private val WEIRD_CONCEPT_ID: Long = 2
    val ICD9_GROUP_CODE = "001"

    fun genderRequest(vararg conceptIds: Long): SearchRequest {
        val searchGroupItem = SearchGroupItem().id("id1").type(DomainType.PERSON.toString())
        for (conceptId in conceptIds) {
            val parameter = SearchParameter()
                    .domain(DomainType.PERSON.toString())
                    .type(CriteriaType.GENDER.toString())
                    .conceptId(conceptId)
                    .group(false)
                    .standard(true)
                    .ancestorData(false)
            searchGroupItem.addSearchParametersItem(parameter)
        }
        return searchRequest(searchGroupItem)
    }

    fun codesRequest(groupType: String, type: String, vararg codes: String): SearchRequest {
        val searchGroupItem = SearchGroupItem().id("id1").type(groupType)
        for (code in codes) {
            val parameter = SearchParameter()
                    .domain(groupType)
                    .type(type)
                    .group(true)
                    .conceptId(1L)
                    .value(code)
                    .standard(false)
                    .ancestorData(false)
            searchGroupItem.addSearchParametersItem(parameter)
        }
        return searchRequest(searchGroupItem)
    }

    fun temporalRequest(): SearchRequest {
        val icd9 = SearchParameter()
                .domain(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD9CM.toString())
                .group(false)
                .conceptId(1L)
                .standard(false)
                .ancestorData(false)
        val icd10 = SearchParameter()
                .domain(Domain.CONDITION.toString())
                .type(CriteriaType.ICD10CM.toString())
                .group(false)
                .conceptId(9L)
                .standard(false)
                .ancestorData(false)
        val snomed = SearchParameter()
                .domain(DomainType.CONDITION.toString())
                .type(CriteriaType.SNOMED.name())
                .group(false)
                .conceptId(4L)
                .standard(false)
                .ancestorData(false)

        val icd9SGI = SearchGroupItem()
                .type(DomainType.CONDITION.toString())
                .addSearchParametersItem(icd9)
                .temporalGroup(0)
        val icd10SGI = SearchGroupItem()
                .type(DomainType.CONDITION.toString())
                .addSearchParametersItem(icd10)
                .temporalGroup(1)
        val snomedSGI = SearchGroupItem()
                .type(DomainType.CONDITION.toString())
                .addSearchParametersItem(snomed)
                .temporalGroup(0)

        // First Mention Of (ICD9Child or Snomed) 5 Days After ICD10
        val temporalGroup = SearchGroup()
                .items(Arrays.asList<T>(icd9SGI, snomedSGI, icd10SGI))
                .temporal(true)
                .mention(TemporalMention.FIRST_MENTION)
                .time(TemporalTime.X_DAYS_AFTER)
                .timeValue(5L)
        return SearchRequest().includes(Arrays.asList<T>(temporalGroup))
    }

    private fun searchRequest(searchGroupItem: SearchGroupItem): SearchRequest {
        val searchGroup = SearchGroup()
        searchGroup.setId("id2")
        searchGroup.setTemporal(false)
        searchGroup.addItemsItem(searchGroupItem)

        val request = SearchRequest()
        request.addIncludesItem(searchGroup)

        return request
    }

    fun icd9Codes(): SearchRequest {
        return codesRequest(
                DomainType.CONDITION.toString(), CriteriaType.ICD9CM.toString(), ICD9_GROUP_CODE)
    }

    fun males(): SearchRequest {
        return genderRequest(MALE_CONCEPT_ID)
    }

    fun females(): SearchRequest {
        return genderRequest(FEMALE_CONCEPT_ID)
    }

    fun maleOrFemale(): SearchRequest {
        return genderRequest(MALE_CONCEPT_ID, FEMALE_CONCEPT_ID)
    }

    fun allGenders(): SearchRequest {
        return genderRequest(MALE_CONCEPT_ID, FEMALE_CONCEPT_ID, WEIRD_CONCEPT_ID)
    }
}
