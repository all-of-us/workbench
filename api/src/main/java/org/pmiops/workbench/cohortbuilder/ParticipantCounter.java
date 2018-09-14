package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import java.util.HashMap;

import org.pmiops.workbench.cdm.DomainTableEnum;
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

    private CohortQueryBuilder cohortQueryBuilder;

  private static final String COUNT_SQL_TEMPLATE =
    "select count(*) as count\n" +
      "from `${projectId}.${dataSetId}.person` person\n" +
      "where\n";

  private static final String ID_SQL_TEMPLATE =
    "select person_id, race_concept_id, gender_concept_id, ethnicity_concept_id, birth_datetime\n" +
      "from `${projectId}.${dataSetId}.person` person\n" +
      "where\n";

  private static final String DEMO_CHART_INFO_SQL_TEMPLATE =
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

  private static final String DOMAIN_CHART_INFO_SQL_TEMPLATE =
    "select concept_name as name, concept_id as conceptId, s.count\n" +
      "from `${projectId}.${dataSetId}.concept`\n" +
      "join (select ${tableId}, count(*) as count\n" +
      "from `${projectId}.${dataSetId}.${table}` person\n" +
      "where\n";

  private static final String DEMO_CHART_INFO_SQL_GROUP_BY =
    "group by gender, race, ageRange\n" +
      "order by gender, race, ageRange\n";

  private static final String LAB_SQL_TEMPLATE =
    "and ${tableId} in (\n" +
      "  select concept_id\n" +
      "  from `${projectId}.${dataSetId}.criteria`\n" +
      "  where type = 'MEAS'\n" +
      "    and subtype = 'LAB'\n" +
      "    and is_selectable = 1\n" +
      ")\n";

  private static final String DOMAIN_CHART_INFO_SQL_GROUP_BY =
    "and ${tableId} != 0\n" +
      "group by ${tableId}\n" +
      "limit ${limit}) as s on concept_id = s.${tableId}\n" +
      "order by count desc, name asc\n";

    private static final String ID_SQL_ORDER_BY = "order by person_id\nlimit";

    private static final String OFFSET_SUFFIX = " offset ";

    private static final String PERSON_TABLE = "person";

    @Autowired
    public ParticipantCounter(CohortQueryBuilder cohortQueryBuilder) {
        this.cohortQueryBuilder = cohortQueryBuilder;
    }

    /**
     * Provides counts of unique subjects
     * defined by the provided {@link ParticipantCriteria}.
     */
    public QueryJobConfiguration buildParticipantCounterQuery(ParticipantCriteria participantCriteria) {
        return buildQuery(participantCriteria, COUNT_SQL_TEMPLATE, "");
    }

    /**
     * Provides counts with demographic info for charts
     * defined by the provided {@link ParticipantCriteria}.
     */
    public QueryJobConfiguration buildDemoChartInfoCounterQuery(ParticipantCriteria participantCriteria) {
        return buildQuery(participantCriteria, DEMO_CHART_INFO_SQL_TEMPLATE, DEMO_CHART_INFO_SQL_GROUP_BY);
    }

    /**
     * Provides counts with domain info for charts
     * defined by the provided {@link ParticipantCriteria}.
     */
    public QueryJobConfiguration buildDomainChartInfoCounterQuery(ParticipantCriteria participantCriteria,
                                                                  DomainType domainType,
                                                                  int chartLimit) {
      String domain = domainType.equals(DomainType.LAB) ? "Measurement" : domainType.name();
      String table = DomainTableEnum.getTableName(domain);
      String tableId = DomainTableEnum.getConceptId(domain);
      if (domain.equals(DomainType.CONDITION.name()) || domain.equals(DomainType.PROCEDURE.name())) {
        tableId = DomainTableEnum.getSourceConceptId(domain);
      }
      String limit = Integer.toString(chartLimit);
      String sqlTemplate = DOMAIN_CHART_INFO_SQL_TEMPLATE
        .replace("${table}", table)
        .replace("${tableId}", tableId);
      String endSqlTemplate = DomainType.LAB.equals(domainType) ?
        LAB_SQL_TEMPLATE + DOMAIN_CHART_INFO_SQL_GROUP_BY :
        DOMAIN_CHART_INFO_SQL_GROUP_BY;
      endSqlTemplate = endSqlTemplate
        .replace("${limit}", limit)
        .replace("${tableId}", tableId);
      return buildQuery(participantCriteria, sqlTemplate, endSqlTemplate);
    }

    public QueryJobConfiguration buildParticipantIdQuery(ParticipantCriteria participantCriteria,
        long resultSize, long offset) {
        String endSql = ID_SQL_ORDER_BY + " " + resultSize;
        if (offset > 0) {
            endSql += OFFSET_SUFFIX + offset;
        }
        return buildQuery(participantCriteria, ID_SQL_TEMPLATE, endSql);
    }

    public QueryJobConfiguration buildQuery(ParticipantCriteria participantCriteria,
        String sqlTemplate, String endSql) {
        return buildQuery(participantCriteria, sqlTemplate, endSql, PERSON_TABLE);
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
        return "when CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE(person.year_of_birth, person.month_of_birth, person.day_of_birth), MONTH)/12) as INT64) >= " + lo +
                " and CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE(person.year_of_birth, person.month_of_birth, person.day_of_birth), MONTH)/12) as INT64) <= " + hi + " then '" + lo + "-" + hi + "'";
    }
}
