package org.pmiops.workbench.cohortreview.querybuilder;

import org.pmiops.workbench.api.CohortReviewController;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.model.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DeviceQueryBuilder implements ReviewQueryBuilder {

  private static final String DEVICES_SQL_TEMPLATE =
    "select de.device_exposure_start_datetime as itemDate,\n" +
      "       c1.vocabulary_id as standardVocabulary,\n" +
      "       c1.concept_name as standardName,\n" +
      "       de.device_source_value as sourceValue,\n" +
      "       c2.vocabulary_id as sourceVocabulary,\n" +
      "       c2.concept_name as sourceName,\n" +
      "       CAST(FLOOR(DATE_DIFF(device_exposure_start_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age\n" +
      "from `${projectId}.${dataSetId}.device_exposure` de\n" +
      "left join `${projectId}.${dataSetId}.concept` c1 on de.device_concept_id = c1.concept_id\n" +
      "left join `${projectId}.${dataSetId}.concept` c2 on de.device_source_concept_id = c2.concept_id\n" +
      "join `${projectId}.${dataSetId}.person` p on de.person_id = p.person_id\n" +
      "where de.person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n" +
      "order by %s %s, device_exposure_id\n" +
      "limit %d offset %d\n";

  private static final String DEVICES_DETAIL_SQL_TEMPLATE =
    "select de.device_exposure_start_datetime as itemDate,\n" +
      "       c1.vocabulary_id as standardVocabulary,\n" +
      "       c1.concept_name as standardName,\n" +
      "       de.device_source_value as sourceValue,\n" +
      "       c2.vocabulary_id as sourceVocabulary,\n" +
      "       c2.concept_name as sourceName,\n" +
      "       CAST(FLOOR(DATE_DIFF(device_exposure_start_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age\n" +
      "from `${projectId}.${dataSetId}.device_exposure` de\n" +
      "left join `${projectId}.${dataSetId}.concept` c1 on de.device_concept_id = c1.concept_id\n" +
      "left join `${projectId}.${dataSetId}.concept` c2 on de.device_source_concept_id = c2.concept_id\n" +
      "join `${projectId}.${dataSetId}.person` p on de.person_id = p.person_id\n" +
      "where de.device_exposure_id = @" + NAMED_DATAID_PARAM + "\n";

  private static final String DEVICES_SQL_COUNT_TEMPLATE =
    "select count(*) as count\n" +
      "from `${projectId}.${dataSetId}.device_exposure`\n" +
      "where person_id = @" + NAMED_PARTICIPANTID_PARAM + "\n";

  @Override
  public String getQuery() {
    return this.DEVICES_SQL_TEMPLATE;
  }

  @Override
  public String getDetailsQuery() {
    return this.DEVICES_DETAIL_SQL_TEMPLATE;
  }

  @Override
  public String getCountQuery() {
    return this.DEVICES_SQL_COUNT_TEMPLATE;
  }

  @Override
  public ParticipantData createParticipantData() {
    return new ParticipantDevice().dataType(DataType.PARTICIPANTDEVICE);
  }

  @Override
  public PageRequest createPageRequest(PageFilterRequest request) {
    String sortColumn = Optional.ofNullable(((ParticipantDevices) request).getSortColumn())
      .orElse(ParticipantDevicesColumns.ITEMDATE).toString();
    int pageParam = Optional.ofNullable(request.getPage()).orElse(CohortReviewController.PAGE);
    int pageSizeParam = Optional.ofNullable(request.getPageSize()).orElse(CohortReviewController.PAGE_SIZE);
    SortOrder sortOrderParam = Optional.ofNullable(request.getSortOrder()).orElse(SortOrder.ASC);
    return new PageRequest(pageParam, pageSizeParam, sortOrderParam, sortColumn);
  }

  @Override
  public PageFilterType getPageFilterType() {
    return PageFilterType.PARTICIPANTDEVICES;
  }
}
