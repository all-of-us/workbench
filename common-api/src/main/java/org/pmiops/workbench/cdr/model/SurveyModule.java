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
public class SurveyModule {

  public static final Function<SurveyModule, org.pmiops.workbench.model.SurveyModule> TO_CLIENT_SURVEY_MODULE =
      (surveyModule) ->
          new org.pmiops.workbench.model.SurveyModule()
              .conceptId(surveyModule.getConceptId())
              .name(surveyModule.getName())
              .description(surveyModule.getDescription())
              .questionCount(surveyModule.getQuestionCount())
              .participantCount(surveyModule.getParticipantCount());

  private long conceptId;
  private String name;
  private String description;
  private long questionCount;
  private long participantCount;

  @Id
  @Column(name = "concept_id")
  public Long getConceptId() {
    return conceptId;
  }

  public void setConceptId(Long conceptId) {
    this.conceptId = conceptId;
  }

  public SurveyModule conceptId(Long conceptId) {
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

  public SurveyModule name(String name) {
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

  public SurveyModule description(String description) {
    this.description = description;
    return this;
  }

  @Column(name="question_count")
  public long getQuestionCount() {
    return questionCount;
  }

  public void setQuestionCount(Long questionCount) {
    this.questionCount = questionCount == null ? 0L : questionCount;
  }

  public SurveyModule questionCount(long questionCount) {
    this.questionCount = questionCount;
    return this;
  }

  @Column(name="participant_count")
  public long getParticipantCount(){
    return participantCount;
  }

  public void setParticipantCount(Long participantCount) {
    this.participantCount = participantCount == null ? 0L : participantCount;
  }

  public SurveyModule participantCount(long participantCount){
    this.participantCount = participantCount;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SurveyModule surveyModule = (SurveyModule) o;
    return Objects.equals(name, surveyModule.name) &&
        Objects.equals(description, surveyModule.description) &&
        Objects.equals(conceptId, surveyModule.conceptId) &&
        Objects.equals(questionCount, surveyModule.questionCount) &&
        Objects.equals(participantCount, surveyModule.participantCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, conceptId, questionCount, participantCount);
  }

  @Override
  public String toString() {
    return  ToStringBuilder.reflectionToString(this);
  }

}
