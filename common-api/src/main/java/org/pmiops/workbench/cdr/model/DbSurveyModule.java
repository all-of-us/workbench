package org.pmiops.workbench.cdr.model;

import java.util.Objects;
import java.util.function.Function;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "survey_module")
public class DbSurveyModule {

  public static final Function<DbSurveyModule, org.pmiops.workbench.model.SurveyModule>
      TO_CLIENT_SURVEY_MODULE =
          (surveyModule) ->
              new org.pmiops.workbench.model.SurveyModule()
                  .conceptId(surveyModule.getConceptId())
                  .name(surveyModule.getName())
                  .description(surveyModule.getDescription())
                  .questionCount(surveyModule.getQuestionCount())
                  .participantCount(surveyModule.getParticipantCount())
                  .orderNumber(surveyModule.getOrderNumber());

  private long conceptId;
  private String name;
  private String description;
  private long questionCount;
  private long participantCount;
  private int orderNumber;

  @Id
  @Column(name = "concept_id")
  public Long getConceptId() {
    return conceptId;
  }

  public void setConceptId(Long conceptId) {
    this.conceptId = conceptId;
  }

  public DbSurveyModule conceptId(Long conceptId) {
    this.conceptId = conceptId;
    return this;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DbSurveyModule name(String name) {
    this.name = name;
    return this;
  }

  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public DbSurveyModule description(String description) {
    this.description = description;
    return this;
  }

  @Column(name = "question_count")
  public long getQuestionCount() {
    return questionCount;
  }

  public void setQuestionCount(Long questionCount) {
    this.questionCount = questionCount == null ? 0L : questionCount;
  }

  public DbSurveyModule questionCount(long questionCount) {
    this.questionCount = questionCount;
    return this;
  }

  @Column(name = "participant_count")
  public long getParticipantCount() {
    return participantCount;
  }

  public void setParticipantCount(Long participantCount) {
    this.participantCount = participantCount == null ? 0L : participantCount;
  }

  public DbSurveyModule participantCount(long participantCount) {
    this.participantCount = participantCount;
    return this;
  }

  @Column(name = "order_number")
  public int getOrderNumber() {
    return orderNumber;
  }

  public void setOrderNumber(int orderNumber) {
    this.orderNumber = orderNumber;
  }

  public DbSurveyModule orderNumber(int orderNumber) {
    this.orderNumber = orderNumber;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbSurveyModule surveyModule = (DbSurveyModule) o;
    return Objects.equals(name, surveyModule.name)
        && Objects.equals(description, surveyModule.description)
        && Objects.equals(conceptId, surveyModule.conceptId)
        && Objects.equals(questionCount, surveyModule.questionCount)
        && Objects.equals(participantCount, surveyModule.participantCount)
        && Objects.equals(orderNumber, surveyModule.orderNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, conceptId, questionCount, participantCount, orderNumber);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
