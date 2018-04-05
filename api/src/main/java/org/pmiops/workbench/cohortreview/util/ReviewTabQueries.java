package org.pmiops.workbench.cohortreview.util;

import org.pmiops.workbench.model.PageFilterType;

public enum ReviewTabQueries {

    CONDITION(PageFilterType.PARTICIPANTCONDITIONS,
            Constants.CONDITIONS_SQL_TEMPLATE,
            Constants.CONDITIONS_SQL_COUNT_TEMPLATE),
    PROCEDURE(PageFilterType.PARTICIPANTPROCEDURES,
            Constants.PROCEDURES_SQL_TEMPLATE,
            Constants.PROCEDURES_SQL_COUNT_TEMPLATE),
    OBSERVATION(PageFilterType.PARTICIPANTOBSERVATIONS,
            Constants.OBSERVATIONS_SQL_TEMPLATE,
            Constants.OBSERVATIONS_SQL_COUNT_TEMPLATE),
    DRUG(PageFilterType.PARTICIPANTDRUGS,
            Constants.DRUGS_SQL_TEMPLATE,
            Constants.DRUGS_SQL_COUNT_TEMPLATE);

    private PageFilterType queryType;
    private String query;
    private String countQuery;
    public static final String NAMED_PARTICIPANTID_PARAM = "participantId";

    private ReviewTabQueries(PageFilterType queryType, String query, String countQuery) {
        this.queryType = queryType;
        this.query = query;
        this.countQuery = countQuery;
    }

    public String getQuery() {
        return this.query;
    }

    public String getCountQuery() {
        return this.countQuery;
    }

    public static ReviewTabQueries fromPageFilterType(PageFilterType queryType) {
        for (ReviewTabQueries queries : ReviewTabQueries.values()) {
            if (queries.queryType.equals(queryType)) {
                return queries;
            }
        }
        return null;
    }

    private static class Constants {

        public static final String CONDITIONS_SQL_TEMPLATE =
                "select co.condition_start_datetime as item_date,\n" +
                        "       c1.vocabulary_id as standard_vocabulary,\n" +
                        "       c1.concept_name as standard_name,\n" +
                        "       co.condition_source_value as source_value,\n" +
                        "       c2.vocabulary_id as source_vocabulary,\n" +
                        "       c2.concept_name as source_name,\n" +
                        "       CAST(FLOOR(DATE_DIFF(condition_start_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age\n" +
                        "from `${projectId}.${dataSetId}.condition_occurrence` co\n" +
                        "left join `${projectId}.${dataSetId}.concept` c1 on co.condition_concept_id = c1.concept_id\n" +
                        "left join `${projectId}.${dataSetId}.concept` c2 on co.condition_source_concept_id = c2.concept_id\n" +
                        "join `${projectId}.${dataSetId}.person` p on co.person_id = p.person_id\n" +
                        "where co.person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
                        "order by %s %s, condition_occurrence_id\n" +
                        "limit %d offset %d\n";

        public static final String PROCEDURES_SQL_TEMPLATE =
                "select po.procedure_datetime as item_date,\n" +
                        "       c1.vocabulary_id as standard_vocabulary,\n" +
                        "       c1.concept_name as standard_name,\n" +
                        "       po.procedure_source_value as source_value,\n" +
                        "       c2.vocabulary_id as source_vocabulary,\n" +
                        "       c2.concept_name as source_name,\n" +
                        "       CAST(FLOOR(DATE_DIFF(procedure_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age\n" +
                        "from `${projectId}.${dataSetId}.procedure_occurrence` po\n" +
                        "left join `${projectId}.${dataSetId}.concept` c1 on po.procedure_concept_id = c1.concept_id\n" +
                        "left join `${projectId}.${dataSetId}.concept` c2 on po.procedure_source_concept_id = c2.concept_id\n" +
                        "join `${projectId}.${dataSetId}.person` p on po.person_id = p.person_id\n" +
                        "where po.person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
                        "order by %s %s, procedure_occurrence_id\n" +
                        "limit %d offset %d\n";

        public static final String OBSERVATIONS_SQL_TEMPLATE =
                "select ob.observation_datetime as item_date,\n" +
                        "       c1.vocabulary_id as standard_vocabulary,\n" +
                        "       c1.concept_name as standard_name,\n" +
                        "       ob.value_as_string as source_value,\n" +
                        "       c2.vocabulary_id as source_vocabulary,\n" +
                        "       c2.concept_name as source_name,\n" +
                        "       CAST(FLOOR(DATE_DIFF(observation_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age\n" +
                        "from `${projectId}.${dataSetId}.observation` ob\n" +
                        "left join `${projectId}.${dataSetId}.concept` c1 on ob.observation_concept_id = c1.concept_id\n" +
                        "left join `${projectId}.${dataSetId}.concept` c2 on ob.observation_source_concept_id = c2.concept_id\n" +
                        "join `${projectId}.${dataSetId}.person` p on ob.person_id = p.person_id\n" +
                        "where ob.person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
                        "order by %s %s, observation_id\n" +
                        "limit %d offset %d\n";

        public static final String DRUGS_SQL_TEMPLATE =
                "select de.drug_exposure_start_datetime as item_date,\n" +
                        "       c1.vocabulary_id as standard_vocabulary,\n" +
                        "       c1.concept_name as standard_name,\n" +
                        "       de.drug_source_value as source_value,\n" +
                        "       de.sig as signature,\n" +
                        "       c2.vocabulary_id as source_vocabulary,\n" +
                        "       c2.concept_name as source_name,\n" +
                        "       CAST(FLOOR(DATE_DIFF(drug_exposure_start_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age\n" +
                        "from `${projectId}.${dataSetId}.drug_exposure` de\n" +
                        "left join `${projectId}.${dataSetId}.concept` c1 on de.drug_concept_id = c1.concept_id\n" +
                        "left join `${projectId}.${dataSetId}.concept` c2 on de.drug_source_concept_id = c2.concept_id\n" +
                        "join `${projectId}.${dataSetId}.person` p on de.person_id = p.person_id\n" +
                        "where de.person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
                        "order by %s %s, drug_exposure_id\n" +
                        "limit %d offset %d\n";

        public static final String CONDITIONS_SQL_COUNT_TEMPLATE =
                "select count(*) as count\n" +
                        "from `${projectId}.${dataSetId}.condition_occurrence`\n" +
                        "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

        public static final String PROCEDURES_SQL_COUNT_TEMPLATE =
                "select count(*) as count\n" +
                        "from `${projectId}.${dataSetId}.procedure_occurrence`\n" +
                        "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

        public static final String OBSERVATIONS_SQL_COUNT_TEMPLATE =
                "select count(*) as count\n" +
                        "from `${projectId}.${dataSetId}.observation`\n" +
                        "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

        public static final String DRUGS_SQL_COUNT_TEMPLATE =
                "select count(*) as count\n" +
                        "from `${projectId}.${dataSetId}.drug_exposure`\n" +
                        "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";
    }
}
