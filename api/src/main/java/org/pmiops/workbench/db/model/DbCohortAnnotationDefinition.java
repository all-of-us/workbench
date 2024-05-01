package org.pmiops.workbench.db.model;

import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.pmiops.workbench.model.AnnotationType;

@Entity
@Table(name = "cohort_annotation_definition")
public class DbCohortAnnotationDefinition {

  private Long cohortAnnotationDefinitionId;
  private int version;
  private Long cohortId;
  private String columnName;
  private Short annotationType;
  private SortedSet<DbCohortAnnotationEnumValue> enumValues = new TreeSet<>();

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "cohort_annotation_definition_id")
  public Long getCohortAnnotationDefinitionId() {
    return cohortAnnotationDefinitionId;
  }

  public void setCohortAnnotationDefinitionId(Long cohortAnnotationDefinitionId) {
    this.cohortAnnotationDefinitionId = cohortAnnotationDefinitionId;
  }

  public DbCohortAnnotationDefinition cohortAnnotationDefinitionId(
      Long cohortAnnotationDefinitionId) {
    this.cohortAnnotationDefinitionId = cohortAnnotationDefinitionId;
    return this;
  }

  @Version
  @Column(name = "version")
  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public DbCohortAnnotationDefinition version(int version) {
    this.version = version;
    return this;
  }

  @Column(name = "cohort_id")
  public Long getCohortId() {
    return cohortId;
  }

  public void setCohortId(Long cohortId) {
    this.cohortId = cohortId;
  }

  public DbCohortAnnotationDefinition cohortId(Long cohortId) {
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

  public DbCohortAnnotationDefinition columnName(String columnName) {
    this.columnName = columnName;
    return this;
  }

  @Column(name = "annotation_type")
  public Short getAnnotationType() {
    return annotationType;
  }

  public void setAnnotationType(Short annotationType) {
    this.annotationType = annotationType;
  }

  public DbCohortAnnotationDefinition annotationType(Short annotationType) {
    this.annotationType = annotationType;
    return this;
  }

  @Transient
  public AnnotationType getAnnotationTypeEnum() {
    return DbStorageEnums.annotationTypeFromStorage(getAnnotationType());
  }

  public void setAnnotationTypeEnum(AnnotationType annotationType) {
    setAnnotationType(DbStorageEnums.annotationTypeToStorage(annotationType));
  }

  public DbCohortAnnotationDefinition annotationTypeEnum(AnnotationType annotationType) {
    return this.annotationType(DbStorageEnums.annotationTypeToStorage(annotationType));
  }

  @OneToMany(
      fetch = FetchType.EAGER,
      mappedBy = "cohortAnnotationDefinition",
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @OrderBy("cohortAnnotationEnumValueId ASC")
  public SortedSet<DbCohortAnnotationEnumValue> getEnumValues() {
    return enumValues;
  }

  public void setEnumValues(SortedSet<DbCohortAnnotationEnumValue> enumValues) {
    this.enumValues = enumValues;
  }

  public DbCohortAnnotationDefinition enumValues(
      SortedSet<DbCohortAnnotationEnumValue> enumValues) {
    this.setEnumValues(enumValues);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbCohortAnnotationDefinition that = (DbCohortAnnotationDefinition) o;
    return version == that.version
        && cohortId == that.cohortId
        && Objects.equals(columnName, that.columnName)
        && annotationType == that.annotationType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, cohortId, columnName, annotationType);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("cohortAnnotationDefinitionId", cohortAnnotationDefinitionId)
        .append("version", version)
        .append("cohortId", cohortId)
        .append("columnName", columnName)
        .append("annotationType", annotationType)
        .toString();
  }
}
