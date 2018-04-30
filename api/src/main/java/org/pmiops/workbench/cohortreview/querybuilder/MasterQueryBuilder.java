package org.pmiops.workbench.cohortreview.querybuilder;

import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.api.CohortReviewController;
import org.pmiops.workbench.cdm.DomainTableEnum;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MasterQueryBuilder is an object that builds sql
 * for all the domain tables in BigQuery.
 */
@Service
public class MasterQueryBuilder implements ReviewQueryBuilder {

  private static final String OUTER_SQL_TEMPLATE =
    "select * from (${masterSqlTemplate})\n" +
      "order by %s %s, dataId\n" +
      "limit %d offset %d\n";

  private static final String MASTER_SQL_TEMPLATE =
    "select t.${tablePrimaryKey} as dataId, " +
      "     t.${tableStartDateTime} as itemDate,\n" +
      "     '${domain}' as domain,\n" +
      "     c1.vocabulary_id as standardVocabulary,\n" +
      "     c1.concept_name as standardName,\n" +
      "     t.${tableSourceValue} as sourceValue,\n" +
      "     c2.vocabulary_id as sourceVocabulary,\n" +
      "     c2.concept_name as sourceName\n" +
      "from `${projectId}.${dataSetId}.${tableName}` t\n" +
      "left join `${projectId}.${dataSetId}.concept` c1 on t.${tableConceptId} = c1.concept_id\n" +
      "left join `${projectId}.${dataSetId}.concept` c2 on t.${tableSourceConceptId} = c2.concept_id\n" +
      "where t.person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

  private static final String UNION = "union all\n";

  private static final String MASTER_SQL_COUNT_TEMPLATE =
    "select count(*) as count\n" +
      "from (${masterSqlTemplate})\n";

  @Override
  public String getQuery() {
    return OUTER_SQL_TEMPLATE.replace("${masterSqlTemplate}", String.join(UNION, buildDomainSql()));
  }

  @Override
  public String getCountQuery() {
    return this.MASTER_SQL_COUNT_TEMPLATE.replace("${masterSqlTemplate}", String.join(UNION, buildDomainSql()));
  }

  @Override
  public ParticipantData createParticipantData() {
    return new ParticipantMaster().dataType(DataType.PARTICIPANTMASTER);
  }

  @Override
  public PageRequest createPageRequest(PageFilterRequest request) {
    String sortColumn = Optional.ofNullable(((ParticipantMasters) request).getSortColumn())
      .orElse(ParticipantMasterColumns.ITEMDATE).toString();
    int pageParam = Optional.ofNullable(request.getPage()).orElse(CohortReviewController.PAGE);
    int pageSizeParam = Optional.ofNullable(request.getPageSize()).orElse(CohortReviewController.PAGE_SIZE);
    SortOrder sortOrderParam = Optional.ofNullable(request.getSortOrder()).orElse(SortOrder.ASC);
    return new PageRequest(pageParam, pageSizeParam, sortOrderParam, sortColumn);
  }

  @Override
  public PageFilterType getPageFilterType() {
    return PageFilterType.PARTICIPANTMASTERS;
  }

  @NotNull
  private List<String> buildDomainSql() {
    List<String> domainSqlList = new ArrayList<>();
    for (DomainTableEnum domainTable : DomainTableEnum.values()) {
      String domainSql =
        this.MASTER_SQL_TEMPLATE
          .replace("${tablePrimaryKey}", DomainTableEnum.getPrimaryKey(domainTable.getDomainId()))
          .replace("${tableStartDateTime}", DomainTableEnum.getEntryDateTime(domainTable.getDomainId()))
          .replace("${domain}", domainTable.getDomainId())
          .replace("${tableSourceValue}", DomainTableEnum.getSourceValue(domainTable.getDomainId()))
          .replace("${tableName}", DomainTableEnum.getTableName(domainTable.getDomainId()))
          .replace("${tableConceptId}", DomainTableEnum.getConceptId(domainTable.getDomainId()))
          .replace("${tableSourceConceptId}", DomainTableEnum.getSourceConceptId(domainTable.getDomainId()));
      domainSqlList.add(domainSql);
    }
    return domainSqlList;
  }
}
