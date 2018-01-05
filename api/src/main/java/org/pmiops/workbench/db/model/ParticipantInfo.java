package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "concept")
public class ParticipantInfo {

    private Long conceptId;
    private String gender;
    private String race;
    private String ethnicity;

    @Id
    @Column(name = "concept_id")
    public Long getConceptId() {
        return conceptId;
    }

    public void setConceptId(Long conceptId) {
        this.conceptId = conceptId;
    }

    public ParticipantInfo conceptId(Long conceptId) {
        this.conceptId = conceptId;
        return this;
    }

    @Column(name = "gender")
    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public ParticipantInfo status(String gender) {
        this.gender = gender;
        return this;
    }

    @Column(name = "race")
    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public ParticipantInfo race(String race) {
        this.race = race;
        return this;
    }

    @Column(name = "ethnicity")
    public String getEthnicity() {
        return ethnicity;
    }

    public void setEthnicity(String ethnicity) {
        this.ethnicity = ethnicity;
    }

    public ParticipantInfo ethnicity(String ethnicity) {
        this.ethnicity = ethnicity;
        return this;
    }
}
