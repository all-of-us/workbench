package org.pmiops.workbench.cohortbuilder.util

import org.pmiops.workbench.cohortbuilder.util.Validation.from
import org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.isEmpty

import com.google.api.client.util.Sets
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import java.util.HashMap
import java.util.Objects
import java.util.stream.Collectors
import org.pmiops.workbench.cdr.dao.CBCriteriaDao
import org.pmiops.workbench.cdr.model.CBCriteria
import org.pmiops.workbench.model.CriteriaSubType
import org.pmiops.workbench.model.CriteriaType
import org.pmiops.workbench.model.DomainType
import org.pmiops.workbench.model.SearchGroup
import org.pmiops.workbench.model.SearchGroupItem
import org.pmiops.workbench.model.SearchParameter
import org.pmiops.workbench.model.SearchRequest

/**
 * This lookup utility extracts all criteria groups in the given search request and produces a
 * lookup map from group parameter to set of matching child concept ids. This class was implemented
 * to consolidate logic between the CB query builders and elastic search filters. The ElasticFilters
 * class will need to be refactored to use this lookup util going forward:
 * TODO:https://precisionmedicineinitiative.atlassian.net/browse/RW-2875
 */
class CriteriaLookupUtil(private val cbCriteriaDao: CBCriteriaDao) {

    private class FullTreeType private constructor(
            internal val domain: DomainType, internal val type: CriteriaType, internal val subType: CriteriaSubType, internal val isStandard: Boolean?) {

        override fun equals(o: Any?): Boolean {
            if (this === o) {
                return true
            }
            if (o == null || javaClass != o.javaClass) {
                return false
            }
            val that = o as CriteriaLookupUtil.FullTreeType?
            return domain === that!!.domain && type === that!!.type && isStandard === that!!.isStandard
        }

        override fun hashCode(): Int {
            return Objects.hash(domain, type, isStandard)
        }

        companion object {

            internal fun fromParam(param: SearchParameter): CriteriaLookupUtil.FullTreeType {
                return CriteriaLookupUtil.FullTreeType(
                        DomainType.valueOf(param.getDomain()),
                        CriteriaType.valueOf(param.getType().toUpperCase()),
                        if (param.getSubtype() == null) null else CriteriaSubType.valueOf(param.getSubtype()),
                        param.getStandard())
            }
        }
    }

    /**
     * Extracts all criteria groups in the given search request and produces a lookup map from group
     * parameter to set of matching child concept ids.
     */
    fun buildCriteriaLookupMap(req: SearchRequest): Map<SearchParameter, Set<Long>> {
        // Three categories of criteria groups are currently supported in a SearchRequest:
        // 1. Groups that need lookup in the ancestor table, e.g. drugs
        // 2. Groups with tree types & a concept ID specified, e.g. ICD9/ICD10/snomed/PPI surveys,
        // questions and answers.
        val childrenByAncestor = Maps.newHashMap<CriteriaLookupUtil.FullTreeType, Map<Long, Set<Long>>>()
        val childrenByParentConcept = Maps.newHashMap<CriteriaLookupUtil.FullTreeType, Map<Long, Set<Long>>>()

        // Within each category/tree type combination, we can batch MySQL requests to translate these
        // groups into child concept IDs. First we build up maps to denote which groups to query and
        // which will eventually hold the results (for now, we mark them with an empty set).
        for (sg in Iterables.concat(req.getIncludes(), req.getExcludes())) {
            for (sgi in sg.getItems()) {
                // Validate that search params exist
                from<Collection>(isEmpty)
                        .test(sgi.getSearchParameters())
                        .throwException("Bad Request: search parameters are empty.")
                for (param in sgi.getSearchParameters()) {
                    if (!param.getGroup() && !param.getAncestorData()) {
                        continue
                    }
                    val treeKey = CriteriaLookupUtil.FullTreeType.fromParam(param)
                    if (param.getAncestorData()) {
                        (childrenByAncestor as java.util.Map<FullTreeType, Map<Long, Set<Long>>>).putIfAbsent(treeKey, Maps.newHashMap())
                        (childrenByAncestor[treeKey] as java.util.Map<Long, Set<Long>>).putIfAbsent(param.getConceptId(), Sets.newHashSet())
                    } else {
                        (childrenByParentConcept as java.util.Map<FullTreeType, Map<Long, Set<Long>>>).putIfAbsent(treeKey, Maps.newHashMap())
                        (childrenByParentConcept[treeKey] as java.util.Map<Long, Set<Long>>)
                                .putIfAbsent(param.getConceptId(), Sets.newHashSet())
                    }
                }
            }
        }

        // Now we get the ancestor concept IDs for each batch.
        for (treeType in childrenByAncestor.keys) {
            val byParent = childrenByAncestor[treeType]
            val parentConceptIds = byParent.keys.stream().map { c -> c!!.toString() }.collect<Set<String>, Any>(Collectors.toSet())

            val parents = Lists.newArrayList<CBCriteria>()
            val leaves = Lists.newArrayList<CBCriteria>()
            // This dao call returns the parents in addition to the criteria ancestors for each leave in
            // order to allow the client to determine the relationship between the returned Criteria.
            cbCriteriaDao
                    .findCriteriaAncestors(
                            treeType.domain.toString(),
                            treeType.type.toString(),
                            CriteriaType.ATC.equals(treeType.type),
                            parentConceptIds)
                    .forEach { c ->
                        if (parentConceptIds.contains(c.conceptId)) {
                            parents.add(c)
                        } else {
                            leaves.add(c)
                        }
                    }

            putLeavesOnParent(byParent, parents, leaves)
        }
        // Now we get the child concept IDs for each batch.
        for (treeType in childrenByParentConcept.keys) {
            val byParent = childrenByParentConcept[treeType]
            val parentConceptIds = byParent.keys.stream().map { c -> c!!.toString() }.collect<Set<String>, Any>(Collectors.toSet())

            val parents = Lists.newArrayList<CBCriteria>()
            val leaves = Lists.newArrayList<CBCriteria>()
            if (treeType.type.equals(CriteriaType.ICD9CM)) {
                // This dao call returns all parents matching the parentConceptIds.
                val ids = cbCriteriaDao
                        .findCriteriaParentsByDomainAndTypeAndParentConceptIds(
                                treeType.domain.toString(),
                                treeType.type.toString(),
                                treeType.isStandard,
                                parentConceptIds)
                        .stream()
                        .map { c -> c.id }
                        .collect<List<Long>, Any>(Collectors.toList())
                // TODO: freemabd this is a temporary fix until we get a real fix for cb_criteria table
                cbCriteriaDao
                        .findCriteriaLeavesAndParentsByDomainAndTypeAndParentIds(
                                treeType.domain.toString(), treeType.type.toString(), ids)
                        .forEach { c ->
                            if (c.group && parentConceptIds.contains(c.conceptId)) {
                                parents.add(c)
                            } else {
                                leaves.add(c)
                            }
                        }
            } else {
                // This dao call returns all parents matching the parentConceptIds.
                val ids = cbCriteriaDao
                        .findCriteriaParentsByDomainAndTypeAndParentConceptIds(
                                treeType.domain.toString(),
                                treeType.type.toString(),
                                treeType.isStandard,
                                parentConceptIds)
                        .stream()
                        .map { c -> c.id.toString() }
                        .collect<String, *>(Collectors.joining(","))
                // Find the entire hierarchy from parent to leaves. Each parent node is now encoded with
                // concept ids. The following lookups are in 2 separate calls for query efficiency
                cbCriteriaDao
                        .findCriteriaLeavesAndParentsByPath(ids)
                        .forEach { c ->
                            if (c.group && parentConceptIds.contains(c.conceptId)) {
                                parents.add(c)
                            } else {
                                leaves.add(c)
                            }
                        }
            }

            putLeavesOnParent(byParent, parents, leaves)
        }

        // Finally, we unpack the results and map them back to the original SearchParameters.
        val builder = HashMap<SearchParameter, Set<Long>>()
        for (sg in Iterables.concat(req.getIncludes(), req.getExcludes())) {
            for (sgi in sg.getItems()) {
                for (param in sgi.getSearchParameters()) {
                    if (!param.getGroup() && !param.getAncestorData()) {
                        continue
                    }
                    val treeKey = CriteriaLookupUtil.FullTreeType.fromParam(param)
                    if (param.getAncestorData()) {
                        builder[param] = childrenByAncestor[treeKey][param.getConceptId()]
                    } else {
                        builder[param] = childrenByParentConcept[treeKey][param.getConceptId()]
                    }
                }
            }
        }
        return builder
    }

    private fun putLeavesOnParent(
            byParent: MutableMap<Long, Set<Long>>, parents: List<CBCriteria>, leaves: List<CBCriteria>) {
        for (c in leaves) {
            // Technically this could scale poorly with many criteria groups. We don't expect this
            // number to be very high as it requires a user action to add a group, but a better data
            // structure could be used here if this becomes too slow.
            for (parent in parents) {
                val path = parent.path
                if (c.path.startsWith(path)) {
                    val parentConceptId = java.lang.Long.parseLong(parent.conceptId)
                    (byParent as java.util.Map<Long, Set<Long>>).putIfAbsent(parentConceptId, Sets.newHashSet())
                    byParent[parentConceptId].add(java.lang.Long.parseLong(c.conceptId))
                }
            }
        }
    }
}
