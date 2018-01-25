package org.pmiops.workbench.db.model;

import javax.persistence.GenerationType;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.pmiops.workbench.model.AnnotationType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "cohort_annotation_definition")
public class CohortAnnotationDefinition {

    private long cohortAnnotationDefinitionId;
    private long cohortId;
    private String columnName;
    private AnnotationType annotationType;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cohort_annotation_definition_id")
    public long getCohortAnnotationDefinitionId() {
        return cohortAnnotationDefinitionId;
    }

    public void setCohortAnnotationDefinitionId(long cohortAnnotationDefinitionId) {
        this.cohortAnnotationDefinitionId = cohortAnnotationDefinitionId;
    }

    public CohortAnnotationDefinition cohortAnnotationDefinitionId(long cohortAnnotationDefinitionId) {
        this.cohortAnnotationDefinitionId = cohortAnnotationDefinitionId;
        return this;
    }

    @Column(name = "cohort_id")
    public long getCohortId() {
        return cohortId;
    }

    public void setCohortId(long cohortId) {
        this.cohortId = cohortId;
    }

    public CohortAnnotationDefinition cohortId(long cohortId) {
        this.cohortId = cohortId;
        return this;
    }

    @Column(name = "column_name")
    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public CohortAnnotationDefinition columnName(String columnName) {
        this.columnName = columnName;
        return this;
    }

    @Column(name = "annotation_type")
    public AnnotationType getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(AnnotationType annotationType) {
        this.annotationType = annotationType;
    }

    public CohortAnnotationDefinition annotationType(AnnotationType annotationType) {
        this.annotationType = annotationType;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CohortAnnotationDefinition that = (CohortAnnotationDefinition) o;
        return cohortId == that.cohortId &&
                Objects.equals(columnName, that.columnName) &&
                annotationType == that.annotationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cohortId, columnName, annotationType);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("cohortAnnotationDefinitionId", cohortAnnotationDefinitionId)
                .append("cohortId", cohortId)
                .append("columnName", columnName)
                .append("annotationType", annotationType)
                .toString();
    }
}
