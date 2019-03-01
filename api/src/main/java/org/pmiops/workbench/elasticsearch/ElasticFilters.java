package org.pmiops.workbench.elasticsearch;

import com.google.api.client.util.Sets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jdk.nashorn.internal.ir.annotations.Immutable;
import joptsimple.internal.Strings;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.model.TreeType;

/**
 * Utility for conversion of Cohort Builder request into Elasticserach filters. Instances of this
 * class are used internally to track metadata during request processing.
 */
public final class ElasticFilters {

  /**
   * ElasticFilterResponse wraps value and attaches some additional metadata regarding the
   * translation of criteria to Elasticsearch query filter. Namely, a query may or may not be
   * an approximation, depending on the contents of the request.
   */
  public static class ElasticFilterResponse<T> {

    private final T value;
    private final boolean isApproximate;

    public ElasticFilterResponse(T value, boolean isApproximate) {
      this.value = value;
      this.isApproximate = isApproximate;
    }

    public T value() {
      return this.value;
    }

    public boolean isApproximate() {
      return this.isApproximate;
    }
  }

  /**
   * Translates a Cohort Builder search request into an Elasticsearch filter. If the request
   * parameters are not supported, this is indicated in the response and a best effort approximation
   * of the target filter is made.
   */
  public static ElasticFilterResponse<QueryBuilder> fromCohortSearch(
      CriteriaDao criteriaDao, SearchRequest req) {
    ElasticFilters f = new ElasticFilters(criteriaDao);
    QueryBuilder q = f.process(req);
    return new ElasticFilterResponse<>(q, f.isApproximate);
  }

  private static Set<TreeType> STANDARD_TREES = ImmutableSet.of(
      TreeType.SNOMED, TreeType.DRUG, TreeType.MEAS, TreeType.VISIT);

  private final CriteriaDao criteriaDao;

  private boolean processed = false;
  private Map<SearchParameter, Set<Long>> childrenByCriteriaGroup;
  private boolean isApproximate = false;

  private ElasticFilters(CriteriaDao criteriaDao) {
    this.criteriaDao = criteriaDao;
  }

  private QueryBuilder process(SearchRequest req) {
    Preconditions.checkArgument(!processed);
    childrenByCriteriaGroup = buildCriteriaGroupLookup(req);

    BoolQueryBuilder filter = QueryBuilders.boolQuery();
    for (SearchGroup sg : req.getIncludes()) {
      filter.filter(searchGroupToFilter(sg));
    }
    for (SearchGroup sg : req.getExcludes()) {
      filter.mustNot(searchGroupToFilter(sg));
    }
    processed = true;
    return filter;
  }

  /**
   * Every criteria ID (one or more concept IDs) in a SearchGroup is effectively OR'd together
   * implemented via a combination of Elastic nested filters and should's. In particular,
   * NUM_OF_OCCURRENCES queries force an independent evaluation of each criteria ID.
   */
  private QueryBuilder searchGroupToFilter(SearchGroup sg) {
    BoolQueryBuilder filter = QueryBuilders.boolQuery();
    if (sg.getTemporal()) {
      this.isApproximate = true;
    }

    for (SearchGroupItem sgi : sg.getItems()) {
      // Note: Later we could investigate starting with an inexpensive presence filter; would need
      // to map each concept ID into the right domain first though.
      BoolQueryBuilder sgiFilter = QueryBuilders.boolQuery();

      // Modifiers apply to all criteria in this SearchGroupItem, but will be reapplied to each
      // subquery generated for each criteria ID.
      List<QueryBuilder> modFilters = Lists.newArrayList();

      // Note: For now we roll all non-trivial modifiers into one standard clause template. It's
      // conceivable applying the "N occurrences" pattern everywhere may cause performance problems;
      // if so, non-occurrence value modifiers can be split out as a separate template.
      int occurredAtLeast = 1;
      for (Modifier mod : sgi.getModifiers()) {
        switch (mod.getName()) {
          case NUM_OF_OCCURRENCES:
            occurredAtLeast = Integer.parseInt(Iterables.getOnlyElement(mod.getOperands()));
            break;
          case EVENT_DATE:
          case AGE_AT_EVENT:
            RangeQueryBuilder rq;
            Object left, right = null;
            if (ModifierType.EVENT_DATE.equals(mod.getName())) {
              rq = QueryBuilders.rangeQuery("events.start_date");
              left = mod.getOperands().get(0);
              if (mod.getOperands().size() > 1) {
                right = mod.getOperands().get(1);
              }
            } else {
              rq = QueryBuilders.rangeQuery("events.age_at_start");
              left = Integer.parseInt(mod.getOperands().get(0));
              if (mod.getOperands().size() > 1) {
                right = Integer.parseInt(mod.getOperands().get(1));
              }
            }
            switch (mod.getOperator()) {
              case LESS_THAN:
                rq.lt(left);
                break;
              case GREATER_THAN:
                rq.gt(left);
                break;
              case LESS_THAN_OR_EQUAL_TO:
                rq.lte(left);
                break;
              case GREATER_THAN_OR_EQUAL_TO:
                rq.gte(left);
                break;
              case BETWEEN:
                rq.gt(left).lt(right);
                break;
              case LIKE:
              case IN:
              case EQUAL:
              case NOT_EQUAL:
              default:
                throw new RuntimeException("Bad operator for date modifier: " + mod.getOperator());
            }
            modFilters.add(rq);
            break;
          case ENCOUNTERS:
            modFilters.add(QueryBuilders.termsQuery("events.visit_concept_id", mod.getOperands()));
            break;
          default:
            throw new RuntimeException("Unknown modifier type: " + mod.getName());
        }
      }
      for (SearchParameter param : sgi.getSearchParameters()) {
        String conceptField = "events." + (isStandardConcept(param) ? "concept_id" : "source_concept_id");
        BoolQueryBuilder b = QueryBuilders.boolQuery()
            .filter(QueryBuilders.termsQuery(conceptField, toleafConceptIds(ImmutableList.of(param))));
        for (QueryBuilder f : modFilters) {
          b.filter(f);
        }
        // "should" gives us "OR" behavior so long as we're in a filter context, which we are. This
        // translates to N occurrences of criteria 1 OR N occurrences of criteria 2, etc.
        sgiFilter.should(
            QueryBuilders.functionScoreQuery(
                QueryBuilders.nestedQuery(
                    // We sum a constant score for each matching document, yielding the total number
                    // of matching nested documents (events).
                    "events", QueryBuilders.constantScoreQuery(b), ScoreMode.Total))
                .setMinScore(occurredAtLeast));
      }
      filter.filter(sgiFilter);
    }

    return filter;
  }

  private static boolean isStandardConcept(SearchParameter param) {
    TreeType paramType = TreeType.valueOf(param.getType());
    return STANDARD_TREES.contains(paramType);
  }

  private Set<String> toleafConceptIds(List<SearchParameter> params) {
    Set<String> out = Sets.newHashSet();
    for (SearchParameter param : params) {
      if (param.getGroup()) {
        out.addAll(
            childrenByCriteriaGroup.get(param)
                .stream()
                .map(id -> Long.toString(id))
                .collect(Collectors.toSet()));
      } else {
        out.add(Long.toString(param.getConceptId()));
      }
    }
    return out;
  }

  private static class FullTreeType {
    final TreeType type;
    final TreeSubType subType;
    private FullTreeType(TreeType type, TreeSubType subType) {
      this.type = type;
      this.subType = subType;
    }

    static FullTreeType fromParam(SearchParameter param) {
      return new FullTreeType(
          TreeType.valueOf(param.getType()), TreeSubType.valueOf(param.getSubtype()));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      FullTreeType that = (FullTreeType) o;
      return type == that.type &&
          subType == that.subType;
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, subType);
    }
  }

  private Map<SearchParameter, Set<Long>> buildCriteriaGroupLookup(SearchRequest req) {
    // Three categories of criteria groups are currently queryable:
    // 1. Groups with only tree types specified, e.g. PPI.
    // 2. Groups with tree types & a criteria code specified, e.g. ICD9/ICD10.
    // 3. Groups with tree types & a concept ID specified, e.g. drugs.
    Map<FullTreeType, Set<Long>> childrenByTreeType = Maps.newHashMap();
    Map<FullTreeType, Map<String, Set<Long>>> childrenByParentCode = Maps.newHashMap();
    Map<FullTreeType, Map<Long, Set<Long>>> childrenByParentConcept = Maps.newHashMap();

    // Within each category/tree type combination, we can batch MySQL requests to translate these
    // groups into child concept IDs. First we build up maps to denote which groups to query and
    // which will eventually hold the results (for now, we mark them with an empty set).
    for (SearchGroup sg : Iterables.concat(req.getIncludes(), req.getExcludes())) {
      for (SearchGroupItem sgi : sg.getItems()) {
        for (SearchParameter param : sgi.getSearchParameters()) {
          if (!param.getGroup()) {
            continue;
          }
          Preconditions.checkArgument(
              !TreeType.SNOMED.toString().equals(param.getType()),
              "SNOMED groups are poly-hierarchical; the SearchRequest does not encode enough " +
                  "information to determine which criteria ID should be used");

          FullTreeType treeKey = FullTreeType.fromParam(param);
          if (param.getConceptId() != null) {
            childrenByParentConcept.putIfAbsent(treeKey, Maps.newHashMap());
            childrenByParentConcept.get(treeKey)
                .putIfAbsent(param.getConceptId(), Sets.newHashSet());
          } else if (param.getValue() != null) {
            childrenByParentCode.putIfAbsent(treeKey, Maps.newHashMap());
            childrenByParentCode.get(treeKey).putIfAbsent(param.getValue(), Sets.newHashSet());
          } else {
            childrenByTreeType.putIfAbsent(treeKey, Sets.newHashSet());
          }
        }
      }
    }

    // Now we get the child concept IDs for each batch.
    for (FullTreeType treeType : childrenByParentConcept.keySet()) {
      Map<Long, Set<Long>> byParent = childrenByParentConcept.get(treeType);
      List<Criteria> criteriaList = criteriaDao.findCriteriaChildrenByTypeAndParentConceptIds(
          treeType.type.toString(), treeType.subType.toString(), byParent.keySet());
      for (Criteria c : criteriaList) {
        byParent.putIfAbsent(c.getParentId(), Sets.newHashSet());
        byParent.get(c.getParentId()).add(Long.parseLong(c.getConceptId()));
      }
    }
    for (FullTreeType treeType : childrenByParentCode.keySet()) {
      Map<String, Set<Long>> byParent = childrenByParentCode.get(treeType);
      List<Criteria> criteriaList = criteriaDao.findCriteriaChildrenByTypeAndParentCodeRegex(
          treeType.type.toString(), treeType.subType.toString(),
          String.format("^(%s)", Strings.join(byParent.keySet(), "|")));
      for (Criteria c : criteriaList) {
        // Technically this could scale poorly with many concept groups. We don't expect this number
        // to be very high as it requires a user action to add a group, but a better data structure
        // could be used here if this becomes too slow.
        for (String parentCode : byParent.keySet()) {
          if (c.getCode().startsWith(parentCode)) {
            byParent.putIfAbsent(parentCode, Sets.newHashSet());
            byParent.get(parentCode).add(Long.parseLong(c.getConceptId()));
          }
        }
      }
    }
    for (FullTreeType treeType : childrenByTreeType.keySet()) {
      childrenByTreeType.get(treeType).addAll(
          criteriaDao.findCriteriaChildrenByType(
              treeType.type.toString(), treeType.subType.toString())
              .stream()
              .map(c -> Long.parseLong(c.getConceptId()))
              .collect(Collectors.toSet()));
    }

    // Finally, we unpack the results and map them back to the original SearchParameters.
    ImmutableMap.Builder<SearchParameter, Set<Long>> builder = ImmutableMap.builder();
    for (SearchGroup sg : Iterables.concat(req.getIncludes(), req.getExcludes())) {
      for (SearchGroupItem sgi : sg.getItems()) {
        for (SearchParameter param : sgi.getSearchParameters()) {
          if (!param.getGroup()) {
            continue;
          }
          FullTreeType treeKey = FullTreeType.fromParam(param);
          if (param.getConceptId() != null) {
            builder.put(param, childrenByParentConcept.get(treeKey).get(param.getConceptId()));
          } else if (param.getValue() != null) {
            builder.put(param, childrenByParentCode.get(treeKey).get(param.getValue()));
          } else {
            builder.put(param, childrenByTreeType.get(treeKey));
          }
        }
      }
    }
    return builder.build();
  }
}
