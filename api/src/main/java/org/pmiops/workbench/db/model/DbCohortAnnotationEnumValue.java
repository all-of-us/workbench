package org.pmiops.workbench.db.model;

import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "cohort_annotation_enum_value")
public class DbCohortAnnotationEnumValue implements Comparable {

  private long cohortAnnotationEnumValueId;
  private String name;
  private int order;
  private DbCohortAnnotationDefinition cohortAnnotationDefinition;
  private DbParticipantCohortAnnotation participantCohortAnnotation;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "cohort_annotation_enum_value_id")
  public long getCohortAnnotationEnumValueId() {
    return cohortAnnotationEnumValueId;
  }

  public void setCohortAnnotationEnumValueId(long cohortAnnotationEnumValueId) {
    this.cohortAnnotationEnumValueId = cohortAnnotationEnumValueId;
  }

  public DbCohortAnnotationEnumValue cohortAnnotationEnumValueId(long cohortAnnotationEnumValueId) {
    this.cohortAnnotationEnumValueId = cohortAnnotationEnumValueId;
    return this;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DbCohortAnnotationEnumValue name(String name) {
    this.name = name;
    return this;
  }

  @Column(name = "enum_order")
  public int getOrder() {
    return order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  public DbCohortAnnotationEnumValue order(int order) {
    this.order = order;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "cohort_annotation_definition_id")
  public DbCohortAnnotationDefinition getCohortAnnotationDefinition() {
    return cohortAnnotationDefinition;
  }

  public void setCohortAnnotationDefinition(
      DbCohortAnnotationDefinition cohortAnnotationDefinition) {
    this.cohortAnnotationDefinition = cohortAnnotationDefinition;
  }

  public DbCohortAnnotationEnumValue cohortAnnotationDefinition(
      DbCohortAnnotationDefinition cohortAnnotationDefinition) {
    this.cohortAnnotationDefinition = cohortAnnotationDefinition;
    return this;
  }

  @OneToOne(mappedBy = "cohortAnnotationEnumValue")
  public DbParticipantCohortAnnotation getParticipantCohortAnnotation() {
    return participantCohortAnnotation;
  }

  public void setParticipantCohortAnnotation(
      DbParticipantCohortAnnotation participantCohortAnnotation) {
    this.participantCohortAnnotation = participantCohortAnnotation;
  }

  public DbCohortAnnotationEnumValue participantCohortAnnotation(
      DbParticipantCohortAnnotation participantCohortAnnotation) {
    this.participantCohortAnnotation = participantCohortAnnotation;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbCohortAnnotationEnumValue enumValue = (DbCohortAnnotationEnumValue) o;
    return Objects.equals(name, enumValue.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("cohortAnnotationEnumValueId", cohortAnnotationEnumValueId)
        .append("name", name)
        .toString();
  }

  @Override
  public int compareTo(Object o) {
    Integer thisOrder = this.getOrder();
    Integer otherOrder = ((DbCohortAnnotationEnumValue) o).getOrder();
    return thisOrder.compareTo(otherOrder);
  }
}
