package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.api.DomainLookupService;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.cohortbuilder.querybuilder.QueryParameters;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Provides counts of unique subjects
 * defined by the provided {@link SearchRequest}.
 */
@Service
public class ParticipantCounter {

    private DomainLookupService domainLookupService;

    private static final String COUNT_SQL_TEMPLATE =
            "select count(distinct person_id) as count\n" +
                    "from `${projectId}.${dataSetId}.person` person\n" +
                    "where\n";

    private static final String ID_SQL_TEMPLATE =
            "select distinct person_id, race_concept_id, gender_concept_id, ethnicity_concept_id, birth_datetime\n" +
                    "from `${projectId}.${dataSetId}.person` person\n" +
                    "where\n";

    private static final String CHART_INFO_SQL_TEMPLATE =
            "select concept1.concept_code as gender, \n" +
                    "case when concept2.concept_name is null then 'Unknown' else concept2.concept_name end as race, \n" +
                    "case " + getAgeRangeSql(0, 18) + "\n" +
                    getAgeRangeSql(19, 44) + "\n" +
                    getAgeRangeSql(45, 64) + "\n" +
                    "else '> 65'\n" +
                    "end as ageRange,\n" +
                    "count(*) as count\n" +
                    "from `${projectId}.${dataSetId}.person` person\n" +
                    "left join `${projectId}.${dataSetId}.concept` concept1 on (person.gender_concept_id = concept1.concept_id and concept1.vocabulary_id = 'Gender')\n" +
                    "left join `${projectId}.${dataSetId}.concept` concept2 on (person.race_concept_id = concept2.concept_id and concept2.vocabulary_id = 'Race')\n" +
                    "where\n";

    private static final String CHART_INFO_SQL_GROUP_BY = "group by gender, race, ageRange\n" + "order by gender, race, ageRange\n";

    private static final String ID_SQL_ORDER_BY = "order by person_id\n" + "limit";

    private static final String OFFSET_SUFFIX = " offset ";

    private static final String UNION_TEMPLATE = "union distinct\n";

    private static final String INCLUDE_SQL_TEMPLATE = "person.person_id in (${includeSql})\n";

    private static final String EXCLUDE_SQL_TEMPLATE =
            "not exists\n" +
                    "(select 'x' from\n" +
                    "(${excludeSql})\n" +
                    "x where x.person_id = person.person_id)\n";

    @Autowired
    public ParticipantCounter(DomainLookupService domainLookupService) {
        this.domainLookupService = domainLookupService;
    }

    /**
     * Provides counts with demographic info for charts
     * defined by the provided {@link SearchRequest}.
     */
    public QueryJobConfiguration buildParticipantCounterQuery(SearchRequest request) {
        domainLookupService.findCodesForEmptyDomains(request.getIncludes());
        domainLookupService.findCodesForEmptyDomains(request.getExcludes());
        return buildQuery(request, COUNT_SQL_TEMPLATE, "");
    }

    /**
     * Provides counts of unique subjects
     * defined by the provided {@link SearchRequest}.
     */
    public QueryJobConfiguration buildChartInfoCounterQuery(SearchRequest request) {
        domainLookupService.findCodesForEmptyDomains(request.getIncludes());
        domainLookupService.findCodesForEmptyDomains(request.getExcludes());
        return buildQuery(request, CHART_INFO_SQL_TEMPLATE, CHART_INFO_SQL_GROUP_BY);
    }

    public QueryJobConfiguration buildParticipantIdQuery(SearchRequest request, long resultSize,
        long offset) {
        domainLookupService.findCodesForEmptyDomains(request.getIncludes());
        domainLookupService.findCodesForEmptyDomains(request.getExcludes());
        String endSql = ID_SQL_ORDER_BY + " " + resultSize;
        if (offset > 0) {
            endSql += OFFSET_SUFFIX + offset;
        }
        return buildQuery(request, ID_SQL_TEMPLATE, endSql);
    }

    public QueryJobConfiguration buildQuery(SearchRequest request, String sqlTemplate, String endSql) {
        if(request.getIncludes().isEmpty() && request.getExcludes().isEmpty()) {
            throw new BadRequestException("Invalid SearchRequest: includes[] and excludes[] cannot both be empty");
        }
        Map<String, QueryParameterValue> params = new HashMap<>();

        // build query for included search groups
        StringJoiner joiner = buildQuery(request.getIncludes(), params, false);

        // if includes is empty then don't add the excludes clause
        if (joiner.toString().isEmpty()) {
            joiner.merge(buildQuery(request.getExcludes(), params, false));
        } else {
            joiner.merge(buildQuery(request.getExcludes(), params, true));
        }

        String finalSql = sqlTemplate + joiner.toString() + endSql;

        return QueryJobConfiguration
                .newBuilder(finalSql)
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build();
    }

    private StringJoiner buildQuery(List<SearchGroup> groups, Map<String, QueryParameterValue> params, Boolean excludeSQL) {
        StringJoiner joiner = new StringJoiner("and ");
        List<String> queryParts = new ArrayList<>();
        for (SearchGroup includeGroup : groups) {
            for (SearchGroupItem includeItem : includeGroup.getItems()) {
                QueryJobConfiguration queryRequest = QueryBuilderFactory
                        .getQueryBuilder(FactoryKey.getType(includeItem.getType()))
                        .buildQueryJobConfig(new QueryParameters()
                                .type(includeItem.getType())
                                .parameters(includeItem.getSearchParameters()));
                params.putAll(queryRequest.getNamedParameters());
                queryParts.add(queryRequest.getQuery());
            }
            if (excludeSQL) {
                joiner.add(EXCLUDE_SQL_TEMPLATE.replace("${excludeSql}", String.join(UNION_TEMPLATE, queryParts)));
            } else {
                joiner.add(INCLUDE_SQL_TEMPLATE.replace("${includeSql}", String.join(UNION_TEMPLATE, queryParts)));
            }
            queryParts = new ArrayList<>();
        }
        return joiner;
    }

    /**
     * Helper method to build sql snippet.
     * @param lo - lower bound of the age range
     * @param hi - upper bound of the age range
     * @return
     */
    private static String getAgeRangeSql(int lo, int hi) {
        return "when CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE(person.year_of_birth, person.month_of_birth, person.day_of_birth), MONTH)/12) as INT64) >= " + lo +
                " and CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE(person.year_of_birth, person.month_of_birth, person.day_of_birth), MONTH)/12) as INT64) <= " + hi + " then '" + lo + "-" + hi + "'";
    }
}
