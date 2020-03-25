package org.pmiops.workbench.elasticsearch;

import com.google.api.client.util.Sets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cohortbuilder.util.CriteriaLookupUtil;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;

/**
 * Utility for conversion of Cohort Builder request into Elasticsearch filters. Instances of this
 * class are used internally to track metadata during request processing.
 */
public final class ElasticFilters {

  private static final Logger log = Logger.getLogger(ElasticFilters.class.getName());

  /** Translates a Cohort Builder search request into an Elasticsearch filter. */
  public static QueryBuilder fromCohortSearch(CBCriteriaDao cbCriteriaDao, SearchRequest req) {
    ElasticFilters f = new ElasticFilters(cbCriteriaDao);
    return f.process(req);
  }

  private static Map<String, String> NON_NESTED_FIELDS =
      ImmutableMap.of(
          CriteriaType.GENDER.toString(), "gender_concept_id",
          CriteriaType.RACE.toString(), "race_concept_id",
          CriteriaType.ETHNICITY.toString(), "ethnicity_concept_id");
  private static List<AttrName> NUMERIC_AGE_TYPES =
      ImmutableList.of(AttrName.AGE_AT_CONSENT, AttrName.AGE_AT_CDR);

  private final CriteriaLookupUtil criteriaLookupUtil;

  private boolean processed = false;
  private Map<SearchParameter, Set<Long>> childrenByCriteriaGroup;

  private ElasticFilters(CBCriteriaDao cbCriteriaDao) {
    this.criteriaLookupUtil = new CriteriaLookupUtil(cbCriteriaDao);
  }

  private QueryBuilder process(SearchRequest req) {
    Preconditions.checkArgument(!processed);
    childrenByCriteriaGroup = criteriaLookupUtil.buildCriteriaLookupMap(req);

    BoolQueryBuilder filter = QueryBuilders.boolQuery();
    for (SearchGroup sg : req.getIncludes()) {
      filter.filter(searchGroupToFilter(sg));
    }
    for (SearchGroup sg : req.getExcludes()) {
      // Only case to use mustNot is when both includes and excludes exist together
      if (req.getIncludes().isEmpty()) {
        filter.filter(searchGroupToFilter(sg));
      } else {
        filter.mustNot(searchGroupToFilter(sg));
      }
    }
    BoolQueryBuilder b = QueryBuilders.boolQuery();
    for (String dataFilter : req.getDataFilters()) {
      b.filter(QueryBuilders.termQuery(dataFilter, true));
    }
    processed = true;
    return req.getDataFilters().isEmpty() ? filter : filter.filter(b);
  }

  /**
   * Every criteria ID (one or more concept IDs) in a SearchGroup is effectively OR'd together
   * implemented via a combination of Elastic nested filters and should's. In particular,
   * NUM_OF_OCCURRENCES queries force an independent evaluation of each criteria ID.
   */
  private QueryBuilder searchGroupToFilter(SearchGroup sg) {
    BoolQueryBuilder filter = QueryBuilders.boolQuery();

    for (SearchGroupItem sgi : sg.getItems()) {
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
            modFilters.add(dateModifierToQuery(mod));
            break;
          case ENCOUNTERS:
            modFilters.add(QueryBuilders.termsQuery("events.visit_concept_id", mod.getOperands()));
            break;
          default:
            throw new BadRequestException("Unknown modifier type: " + mod.getName());
        }
      }

      // TODO(freemabd): Handle Blood Pressure
      for (SearchParameter param : sgi.getSearchParameters()) {
        String conceptField =
            "events." + (param.getStandard() ? "concept_id" : "source_concept_id");
        if (isNonNestedSchema(param)) {
          conceptField = NON_NESTED_FIELDS.get(param.getType());
        }
        Set<String> leafConceptIds = toleafConceptIds(ImmutableList.of(param));
        BoolQueryBuilder b = QueryBuilders.boolQuery();
        if (!leafConceptIds.isEmpty()) {
          b.filter(QueryBuilders.termsQuery(conceptField, leafConceptIds));
        } else {
          // should represent a deceased query
          boolean isDeceased = CriteriaType.valueOf(param.getType()).equals(CriteriaType.DECEASED);
          b.filter(QueryBuilders.termQuery("is_deceased", isDeceased));
        }
        for (Attribute attr : param.getAttributes()) {
          b.filter(attributeToQuery(attr, DomainType.SURVEY.toString().equals(param.getDomain())));
        }
        for (QueryBuilder f : modFilters) {
          b.filter(f);
        }

        if (isNonNestedSchema(param)) {
          // setup non nested filter with proper field
          filter.should(b);
        } else {
          // "should" gives us "OR" behavior so long as we're in a filter context, which we are.
          // This translates to N occurrences of criteria 1 OR N occurrences of criteria 2, etc.
          filter.should(
              QueryBuilders.functionScoreQuery(
                      QueryBuilders.nestedQuery(
                          // We sum a constant score for each matching document, yielding the total
                          // number of matching nested documents (events).
                          "events", QueryBuilders.constantScoreQuery(b), ScoreMode.Total))
                  .setMinScore(occurredAtLeast));
        }
      }
    }

    return filter;
  }

  private static QueryBuilder attributeToQuery(Attribute attr, boolean isSourceConceptId) {
    // Attributes with a name of CAT map to the value_as_concept_id column
    if (AttrName.CAT.equals(attr.getName())) {
      // Currently the UI only uses the In operator for CAT which fits the terms query
      String name =
          isSourceConceptId ? "events.value_as_source_concept_id" : "events.value_as_concept_id";
      return QueryBuilders.termsQuery(name, attr.getOperands());
    }
    Object left = null, right = null;
    RangeQueryBuilder rq;
    if (AttrName.NUM.equals(attr.getName())) {
      rq = QueryBuilders.rangeQuery("events.value_as_number");
      left = Float.parseFloat(attr.getOperands().get(0));
      if (attr.getOperands().size() > 1) {
        right = Float.parseFloat(attr.getOperands().get(1));
      }
      switch (attr.getOperator()) {
        case LESS_THAN_OR_EQUAL_TO:
          rq.lte(left);
          break;
        case GREATER_THAN_OR_EQUAL_TO:
          rq.gte(left);
          break;
        case BETWEEN:
          rq.gte(left).lte(right);
          break;
        case EQUAL:
          rq.gte(left).lte(left);
          break;
        default:
          throw new BadRequestException("Bad operator for attribute: " + attr.getOperator());
      }
      return rq;
    }
    if (AttrName.AGE.equals(attr.getName())) {
      rq = QueryBuilders.rangeQuery("birth_datetime");
      // use the low end of the age range to calculate the high end(right) of the date range
      right = ElasticUtils.todayMinusYears(Integer.parseInt(attr.getOperands().get(0)));
      if (attr.getOperands().size() > 1) {
        // use high end of the age range to calculate the low end(left) of the date range
        // need to add 1 year to adjust to the beginning of the date range
        // Ex: 2019-03-19(current date) - 55year(age) - 1 year = 1963-03-19
        // Need to use GT to make sure not to include 1963-03-19 which evaluates to 56 years old
        // which is out the range of 55. 1963-03-20 evaluates to 55 years 11 months 30 days.
        left = ElasticUtils.todayMinusYears(Integer.parseInt(attr.getOperands().get(1)) + 1);
      }
      switch (attr.getOperator()) {
        case BETWEEN:
          rq.gte(left).lte(right).format("yyyy-MM-dd");
          break;
        default:
          throw new BadRequestException("Bad operator for attribute: " + attr.getOperator());
      }
      return rq;
    }
    if (NUMERIC_AGE_TYPES.contains(attr.getName())) {
      rq =
          QueryBuilders.rangeQuery(
              AttrName.AGE_AT_CONSENT.equals(attr.getName()) ? "age_at_consent" : "age_at_cdr");
      left = Integer.parseInt(attr.getOperands().get(0));
      if (attr.getOperands().size() > 1) {
        right = Integer.parseInt(attr.getOperands().get(1));
      }
      switch (attr.getOperator()) {
        case BETWEEN:
          rq.gte(left).lte(right);
          break;
        default:
          throw new BadRequestException("Bad operator for attribute: " + attr.getOperator());
      }
      return rq;
    }
    throw new BadRequestException("Attribute name is not an attr name type: " + attr.getName());
  }

  private static QueryBuilder dateModifierToQuery(Modifier mod) {
    RangeQueryBuilder rq;
    Object left, right = null;
    if (ModifierType.EVENT_DATE.equals(mod.getName())) {
      rq = QueryBuilders.rangeQuery("events.start_date");
      left = mod.getOperands().get(0);
      if (mod.getOperands().size() > 1) {
        right = mod.getOperands().get(1);
      }
    } else if (ModifierType.AGE_AT_EVENT.equals(mod.getName())) {
      rq = QueryBuilders.rangeQuery("events.age_at_start");
      left = Integer.parseInt(mod.getOperands().get(0));
      if (mod.getOperands().size() > 1) {
        right = Integer.parseInt(mod.getOperands().get(1));
      }
    } else {
      throw new RuntimeException("modifier is not a date modifier type: " + mod.getName());
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
        rq.gte(left).lte(right);
        break;
      case LIKE:
      case IN:
      case EQUAL:
      case NOT_EQUAL:
      default:
        throw new BadRequestException("Bad operator for date modifier: " + mod.getOperator());
    }
    return rq;
  }

  private static boolean isNonNestedSchema(SearchParameter param) {
    DomainType domainType = DomainType.valueOf(param.getDomain());
    return DomainType.PERSON.equals(domainType);
  }

  private Set<String> toleafConceptIds(List<SearchParameter> params) {
    Set<String> out = Sets.newHashSet();
    for (SearchParameter param : params) {
      if (param.getGroup()) {
        out.addAll(
            childrenByCriteriaGroup.get(param).stream()
                .map(id -> Long.toString(id))
                .collect(Collectors.toSet()));
      }
      if (param.getConceptId() != null) {
        // not all SearchParameter have a concept id, so attributes/modifiers
        // are used to find matches in those scenarios.
        out.add(Long.toString(param.getConceptId()));
      }
    }
    return out;
  }
}
