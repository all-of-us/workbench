package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import java.util.HashMap;

import org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Provides counts of unique subjects
 * defined by the provided {@link SearchRequest}.
 */
@Service
public class ParticipantCounter {

  public static final String STANDARD_CONCEPT_ID = "standard_concept_id";
  private CohortQueryBuilder cohortQueryBuilder;
  private static final String TABLE_PREFIX = QueryBuilderConstants.TABLE_PREFIX;

  private static final String PERSON_TABLE = "person";

  private static final String SEARCH_PERSON_TABLE = "search_person";

  private static final String COUNT_SQL_TEMPLATE =
    "select count(*) as count\n" +
      "from `${projectId}.${dataSetId}.${table}` ${table}\n" +
      "where\n";

  private static final String ID_SQL_TEMPLATE =
    "select rand() as x, person_id, race_concept_id, gender_concept_id, ethnicity_concept_id, birth_datetime\n" +
      "from `${projectId}.${dataSetId}.${table}` ${table}\n" +
      "where\n";

  private static final String DEMO_CHART_INFO_SQL_TEMPLATE =
    "select gender, \n" +
      "race, \n" +
      "case " + getAgeRangeSql(0, 18) + "\n" +
      getAgeRangeSql(19, 44) + "\n" +
      getAgeRangeSql(45, 64) + "\n" +
      "else '> 65'\n" +
      "end as ageRange,\n" +
      "count(*) as count\n" +
      "from `${projectId}.${dataSetId}.${table}` ${table}\n" +
      "where\n";

  private static final String DOMAIN_CHART_INFO_SQL_TEMPLATE =
    "select standard_name as name, standard_concept_id as conceptId, count(distinct person_id) as count\n" +
      "from `${projectId}.${dataSetId}.${table}` ${table}\n" +
      "where\n";

  private static final String DEMO_CHART_INFO_SQL_GROUP_BY =
    "group by gender, race, ageRange\n" +
      "order by gender, race, ageRange\n";

  private static final String DOMAIN_CHART_INFO_SQL_GROUP_BY =
    "and standard_concept_id != 0 \n" +
      "group by name, conceptId\n" +
      "order by count desc, name asc\n" +
      "limit ${limit}\n";

    private static final String ID_SQL_ORDER_BY = "order by x\nlimit";

    private static final String OFFSET_SUFFIX = " offset ";

    @Autowired
    public ParticipantCounter(CohortQueryBuilder cohortQueryBuilder) {
        this.cohortQueryBuilder = cohortQueryBuilder;
    }

    /**
     * Provides counts of unique subjects
     * defined by the provided {@link ParticipantCriteria}.
     */
    public QueryJobConfiguration buildParticipantCounterQuery(ParticipantCriteria participantCriteria) {
        return buildQuery(participantCriteria, COUNT_SQL_TEMPLATE.replace("${table}", SEARCH_PERSON_TABLE),
          "", SEARCH_PERSON_TABLE);
    }

    /**
     * Provides counts with demographic info for charts
     * defined by the provided {@link ParticipantCriteria}.
     */
    public QueryJobConfiguration buildDemoChartInfoCounterQuery(ParticipantCriteria participantCriteria) {
        return buildQuery(participantCriteria, DEMO_CHART_INFO_SQL_TEMPLATE.replace("${table}", SEARCH_PERSON_TABLE),
          DEMO_CHART_INFO_SQL_GROUP_BY, SEARCH_PERSON_TABLE);
    }

    /**
     * Provides counts with domain info for charts
     * defined by the provided {@link ParticipantCriteria}.
     */
    public QueryJobConfiguration buildDomainChartInfoCounterQuery(ParticipantCriteria participantCriteria,
                                                                  DomainType domainType,
                                                                  int chartLimit) {
      String table = TABLE_PREFIX + domainType.name().toLowerCase();
      String limit = Integer.toString(chartLimit);
      String sqlTemplate = DOMAIN_CHART_INFO_SQL_TEMPLATE
        .replace("${table}", table);
      String endSqlTemplate = DOMAIN_CHART_INFO_SQL_GROUP_BY;
      endSqlTemplate = endSqlTemplate
        .replace("${limit}", limit)
        .replace("${tableId}", STANDARD_CONCEPT_ID);
      return buildQuery(participantCriteria, sqlTemplate, endSqlTemplate, table);
    }

  public QueryJobConfiguration buildParticipantIdQuery(ParticipantCriteria participantCriteria,
                                                       long resultSize,
                                                       long offset) {
        String endSql = ID_SQL_ORDER_BY + " " + resultSize;
        if (offset > 0) {
            endSql += OFFSET_SUFFIX + offset;
        }
        return buildQuery(participantCriteria, ID_SQL_TEMPLATE.replace("${table}", PERSON_TABLE),
          endSql, PERSON_TABLE);
    }

    public QueryJobConfiguration buildQuery(ParticipantCriteria participantCriteria,
        String sqlTemplate, String endSql, String mainTable) {
        return cohortQueryBuilder.buildQuery(participantCriteria, sqlTemplate, endSql, mainTable,
            new HashMap<>());
    }


    /**
     * Helper method to build sql snippet.
     * @param lo - lower bound of the age range
     * @param hi - upper bound of the age range
     * @return
     */
    private static String getAgeRangeSql(int lo, int hi) {
      return "when CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE(" + SEARCH_PERSON_TABLE + ".dob), MONTH)/12) as INT64) >= " + lo +
        " and CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE(" + SEARCH_PERSON_TABLE + ".dob), MONTH)/12) as INT64) <= " + hi + " then '" + lo + "-" + hi + "'";
    }
}
