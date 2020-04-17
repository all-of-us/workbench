package org.pmiops.workbench.cohortbuilder.util;

import static org.pmiops.workbench.cohortbuilder.util.Validation.from;
import static org.pmiops.workbench.cohortbuilder.util.ValidationPredicates.isEmpty;

import com.google.api.client.util.Sets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.model.DbCriteriaLookup;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;

/**
 * This lookup utility extracts all criteria groups in the given search request and produces a
 * lookup map from group parameter to set of matching child concept ids. This class was implemented
 * to consolidate logic between the CB query builders and elastic search filters. The ElasticFilters
 * class will need to be refactored to use this lookup util going forward:
 * TODO:https://precisionmedicineinitiative.atlassian.net/browse/RW-2875
 */
public final class CriteriaLookupUtil {

  private final CBCriteriaDao cbCriteriaDao;

  private static class FullTreeType {
    final DomainType domain;
    final CriteriaType type;
    final CriteriaSubType subType;
    final Boolean isStandard;

    private FullTreeType(
        DomainType domain, CriteriaType type, CriteriaSubType subType, Boolean isStandard) {
      this.domain = domain;
      this.type = type;
      this.subType = subType;
      this.isStandard = isStandard;
    }

    static CriteriaLookupUtil.FullTreeType fromParam(SearchParameter param) {
      return new CriteriaLookupUtil.FullTreeType(
          DomainType.valueOf(param.getDomain()),
          CriteriaType.valueOf(param.getType().toUpperCase()),
          param.getSubtype() == null ? null : CriteriaSubType.valueOf(param.getSubtype()),
          param.getStandard());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CriteriaLookupUtil.FullTreeType that = (CriteriaLookupUtil.FullTreeType) o;
      return domain == that.domain && type == that.type && isStandard == that.isStandard;
    }

    @Override
    public int hashCode() {
      return Objects.hash(domain, type, isStandard);
    }
  }

  public CriteriaLookupUtil(CBCriteriaDao cbCriteriaDao) {
    this.cbCriteriaDao = cbCriteriaDao;
  }

  /**
   * Extracts all criteria groups in the given search request and produces a lookup map from group
   * parameter to set of matching child concept ids.
   */
  public Map<SearchParameter, Set<Long>> buildCriteriaLookupMap(SearchRequest req) {
    // Three categories of criteria groups are currently supported in a SearchRequest:
    // 1. Groups that need lookup in the ancestor table, e.g. drugs
    // 2. Groups with tree types & a concept ID specified, e.g. ICD9/ICD10/snomed/PPI surveys,
    // questions and answers.
    Map<CriteriaLookupUtil.FullTreeType, Map<Long, Set<Long>>> childrenByAncestor =
        Maps.newHashMap();
    Map<CriteriaLookupUtil.FullTreeType, Map<Long, Set<Long>>> childrenByParentConcept =
        Maps.newHashMap();

    // Within each category/tree type combination, we can batch MySQL requests to translate these
    // groups into child concept IDs. First we build up maps to denote which groups to query and
    // which will eventually hold the results (for now, we mark them with an empty set).
    for (SearchGroup sg : Iterables.concat(req.getIncludes(), req.getExcludes())) {
      for (SearchGroupItem sgi : sg.getItems()) {
        // Validate that search params exist
        from(isEmpty())
            .test(sgi.getSearchParameters())
            .throwException("Bad Request: search parameters are empty.");
        for (SearchParameter param : sgi.getSearchParameters()) {
          if (!param.getGroup() && !param.getAncestorData()) {
            continue;
          }
          CriteriaLookupUtil.FullTreeType treeKey =
              CriteriaLookupUtil.FullTreeType.fromParam(param);
          if (param.getAncestorData()) {
            childrenByAncestor.putIfAbsent(treeKey, Maps.newHashMap());
            childrenByAncestor.get(treeKey).putIfAbsent(param.getConceptId(), Sets.newHashSet());
          } else {
            childrenByParentConcept.putIfAbsent(treeKey, Maps.newHashMap());
            childrenByParentConcept
                .get(treeKey)
                .putIfAbsent(param.getConceptId(), Sets.newHashSet());
          }
        }
      }
    }

    // Now we get the ancestor concept IDs for each batch.
    for (CriteriaLookupUtil.FullTreeType treeType : childrenByAncestor.keySet()) {
      Map<Long, Set<Long>> byParent = childrenByAncestor.get(treeType);
      Set<String> parentConceptIds =
          byParent.keySet().stream().map(c -> c.toString()).collect(Collectors.toSet());

      List<DbCriteriaLookup> parents = Lists.newArrayList();
      List<DbCriteriaLookup> leaves = Lists.newArrayList();
      // This dao call returns the parents in addition to the criteria ancestors for each leave in
      // order to allow the client to determine the relationship between the returned Criteria.
      cbCriteriaDao
          .findCriteriaAncestors(
              treeType.domain.toString(),
              treeType.type.toString(),
              CriteriaType.ATC.equals(treeType.type),
              parentConceptIds)
          .forEach(
              c -> {
                if (parentConceptIds.contains(String.valueOf(c.getConceptId()))) {
                  parents.add(c);
                } else {
                  leaves.add(c);
                }
              });

      putLeavesOnParent(byParent, parents, leaves);
    }
    // Now we get the child concept IDs for each batch.
    for (CriteriaLookupUtil.FullTreeType treeType : childrenByParentConcept.keySet()) {
      Map<Long, Set<Long>> byParent = childrenByParentConcept.get(treeType);
      Set<String> parentConceptIds =
          byParent.keySet().stream().map(c -> c.toString()).collect(Collectors.toSet());

      List<DbCriteriaLookup> parents = Lists.newArrayList();
      List<DbCriteriaLookup> leaves = Lists.newArrayList();
      if (treeType.type.equals(CriteriaType.ICD9CM)) {
        // This dao call returns all parents matching the parentConceptIds.
        List<Long> ids =
            cbCriteriaDao
                .findCriteriaParentsByDomainAndTypeAndParentConceptIds(
                    treeType.domain.toString(),
                    treeType.type.toString(),
                    treeType.isStandard,
                    parentConceptIds)
                .stream()
                .map(c -> c.getId())
                .collect(Collectors.toList());
        // TODO: freemabd this is a temporary fix until we get a real fix for cb_criteria table
        cbCriteriaDao
            .findCriteriaLeavesAndParentsByDomainAndTypeAndParentIds(
                treeType.domain.toString(), treeType.type.toString(), ids)
            .forEach(
                c -> {
                  if (parentConceptIds.contains(String.valueOf(c.getConceptId()))) {
                    parents.add(c);
                  } else {
                    leaves.add(c);
                  }
                });
      } else {
        // This dao call returns all parents matching the parentConceptIds.
        String ids =
            cbCriteriaDao
                .findCriteriaParentsByDomainAndTypeAndParentConceptIds(
                    treeType.domain.toString(),
                    treeType.type.toString(),
                    treeType.isStandard,
                    parentConceptIds)
                .stream()
                .map(c -> String.valueOf(c.getId()))
                .collect(Collectors.joining(","));
        // Find the entire hierarchy from parent to leaves. Each parent node is now encoded with
        // concept ids. The following lookups are in 2 separate calls for query efficiency
        List<Long> longIds =
            Stream.of(ids.split(",")).map(Long::parseLong).collect(Collectors.toList());
        cbCriteriaDao
            .findCriteriaLeavesAndParentsByPath(longIds, ids)
            .forEach(
                c -> {
                  if (parentConceptIds.contains(String.valueOf(c.getConceptId()))) {
                    parents.add(c);
                  } else {
                    leaves.add(c);
                  }
                });
      }

      putLeavesOnParent(byParent, parents, leaves);
    }

    // Finally, we unpack the results and map them back to the original SearchParameters.
    Map<SearchParameter, Set<Long>> builder = new HashMap<>();
    for (SearchGroup sg : Iterables.concat(req.getIncludes(), req.getExcludes())) {
      for (SearchGroupItem sgi : sg.getItems()) {
        for (SearchParameter param : sgi.getSearchParameters()) {
          if (!param.getGroup() && !param.getAncestorData()) {
            continue;
          }
          CriteriaLookupUtil.FullTreeType treeKey =
              CriteriaLookupUtil.FullTreeType.fromParam(param);
          if (param.getAncestorData()) {
            builder.put(param, childrenByAncestor.get(treeKey).get(param.getConceptId()));
          } else {
            builder.put(param, childrenByParentConcept.get(treeKey).get(param.getConceptId()));
          }
        }
      }
    }
    return builder;
  }

  private void putLeavesOnParent(
      Map<Long, Set<Long>> byParent,
      List<DbCriteriaLookup> parents,
      List<DbCriteriaLookup> leaves) {
    for (DbCriteriaLookup c : leaves) {
      // Technically this could scale poorly with many criteria groups. We don't expect this
      // number to be very high as it requires a user action to add a group, but a better data
      // structure could be used here if this becomes too slow.
      for (DbCriteriaLookup parent : parents) {
        if (c.getParentId().equals(parent.getParentId())) {
          if (c.getConceptId() != null) {
            long parentConceptId = parent.getConceptId();
            byParent.putIfAbsent(parentConceptId, Sets.newHashSet());
            byParent.get(parentConceptId).add(c.getConceptId());
          }
        }
      }
    }
  }
}
