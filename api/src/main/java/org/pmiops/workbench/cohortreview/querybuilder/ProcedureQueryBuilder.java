package org.pmiops.workbench.cohortreview.querybuilder;

import org.pmiops.workbench.api.CohortReviewController;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.model.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * ProcedureQueryBuilder is an object that builds sql
 * for the Procedure Occurrences table in BigQuery.
 */
@Service
public class ProcedureQueryBuilder implements ReviewQueryBuilder {

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

    public static final String PROCEDURES_SQL_COUNT_TEMPLATE =
            "select count(*) as count\n" +
                    "from `${projectId}.${dataSetId}.procedure_occurrence`\n" +
                    "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

    @Override
    public String getQuery() {
        return this.PROCEDURES_SQL_TEMPLATE;
    }

    @Override
    public String getCountQuery() {
        return this.PROCEDURES_SQL_COUNT_TEMPLATE;
    }

    @Override
    public ParticipantData createParticipantData() {
        return new ParticipantProcedure().dataType(DataType.PARTICIPANTPROCEDURE);
    }

    @Override
    public PageRequest createPageRequest(PageFilterRequest request) {
        String sortColumn =  Optional.ofNullable(((ParticipantProcedures) request).getSortColumn())
                .orElse(ParticipantProceduresColumns.ITEMDATE).toString();
        int pageParam = Optional.ofNullable(request.getPage()).orElse(CohortReviewController.PAGE);
        int pageSizeParam = Optional.ofNullable(request.getPageSize()).orElse(CohortReviewController.PAGE_SIZE);
        SortOrder sortOrderParam = Optional.ofNullable(request.getSortOrder()).orElse(SortOrder.ASC);
        return new PageRequest(pageParam, pageSizeParam, sortOrderParam, sortColumn);
    }

    @Override
    public PageFilterType getPageFilterType() {
        return PageFilterType.PARTICIPANTPROCEDURES;
    }
}
