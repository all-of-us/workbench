package org.pmiops.workbench.cohortreview;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AnnotationQuery;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class AnnotationQueryBuilder {

  private static final String PERSON_ID_COLUMN = "person_id";
  private static final String REVIEW_STATUS_COLUMN = "review_status";

  private static final ImmutableMap<AnnotationType, String> ANNOTATION_COLUMN_MAP =
      ImmutableMap.of(
          AnnotationType.BOOLEAN, "annotation_value_boolean",
          AnnotationType.DATE, "annotation_value_date",
          AnnotationType.INTEGER, "annotation_value_integer",
          AnnotationType.STRING, "annotation_value_string");

  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
  private final CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;

  @Autowired
  AnnotationQueryBuilder(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
      CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao) {
    this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    this.cohortAnnotationDefinitionDao = cohortAnnotationDefinitionDao;
  }

  private static final String ANNOTATION_JOIN_SQL =
      " LEFT OUTER JOIN participant_cohort_annotations a%d "
      + "ON a%d.participant_id = pcs.participant_id "
      + "AND a%d.cohort_annotation_definition_id = %d"
      + "AND a%d.cohort_review_id = %d";

  private static final String ANNOTATION_VALUE_JOIN_SQL =
      " LEFT OUTER JOIN cohort_annotation_enum_value ae%d "
      + "ON ae%d.cohort_annotation_enum_value_id = a%d.cohort_annotation_enum_value_id";

  private static final String WHERE_SQL =
      " WHERE pcs.cohort_review_id = %d";

  private static final ImmutableSet<CohortStatus> REVIEWED_STATUSES =
      ImmutableSet.of(CohortStatus.INCLUDED, CohortStatus.EXCLUDED, CohortStatus.NEEDS_FURTHER_REVIEW);

  public Iterable<Map<String, Object>> materializeAnnotationQuery(CohortReview cohortReview,
      List<CohortStatus> statusFilter,
      AnnotationQuery annotationQuery, int limit, long offset) {
    Map<String, CohortAnnotationDefinition> annotationDefinitions = null;
    List<String> columns = annotationQuery.getColumns();
    StringBuilder selectBuilder = new StringBuilder("SELECT ");
    StringBuilder fromBuilder = new StringBuilder(" FROM participant_cohort_status pcs");
    int annotationCount = 0;
    boolean firstColumn = true;
    for (String column : columns) {
      if (firstColumn) {
        firstColumn = false;
      } else {
        selectBuilder.append(", ");
      }
      if (column.equals(PERSON_ID_COLUMN)) {
        selectBuilder.append("pcs.participant_id person_id");
      } else if (column.equals(REVIEW_STATUS_COLUMN)) {
        selectBuilder.append("pcs.status review_status");
      } else {
        if (annotationDefinitions == null) {
          annotationDefinitions =
              Maps.uniqueIndex(
                  cohortAnnotationDefinitionDao.findByCohortId(cohortReview.getCohortId()),
                  CohortAnnotationDefinition::getColumnName);

        }
        CohortAnnotationDefinition definition = annotationDefinitions.get(column);
        if (definition == null) {
          throw new BadRequestException("Invalid annotation name: " + column);
        }
        annotationCount++;
        fromBuilder.append(
            String.format(ANNOTATION_JOIN_SQL, annotationCount, annotationCount, annotationCount,
                definition.getCohortAnnotationDefinitionId(), cohortReview.getCohortReviewId()));
        if (definition.getAnnotationType().equals(AnnotationType.ENUM)) {
          selectBuilder.append(String.format("ae%d.name av%d", annotationCount, annotationCount));
          fromBuilder.append(
              String.format(ANNOTATION_VALUE_JOIN_SQL, annotationCount, annotationCount, annotationCount));
        } else {
          String columnName = ANNOTATION_COLUMN_MAP.get(definition.getAnnotationType());
          if (columnName == null) {
            throw new BadRequestException("Invalid annotation type: " + definition.getAnnotationType());
          }
          selectBuilder.append(String.format("a%d.%s av%d", annotationCount, columnName, annotationCount));
        }
      }
    }
    StringBuilder whereBuilder = new StringBuilder(
        String.format(" WHERE pcs.cohort_review_id = %d", cohortReview.getCohortReviewId()));
    if (!statusFilter.containsAll(REVIEWED_STATUSES)) {
      whereBuilder.append(" AND pcs.status IN (");
      whereBuilder.append(Joiner.on(", ").join(statusFilter.stream().map(CohortStatus::ordinal).toArray()));
      whereBuilder.append(")");
    }
    StringBuilder sqlBuilder = new StringBuilder(selectBuilder);
    sqlBuilder.append(fromBuilder);


    return null;
  }
}
