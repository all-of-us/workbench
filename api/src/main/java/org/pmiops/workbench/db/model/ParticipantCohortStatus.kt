package org.pmiops.workbench.db.model

import java.sql.Date
import java.util.Objects
import javax.persistence.AttributeOverride
import javax.persistence.AttributeOverrides
import javax.persistence.Column
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.Transient
import org.apache.commons.lang3.builder.ToStringBuilder
import org.pmiops.workbench.model.CohortStatus

@Entity
@Table(name = "participant_cohort_status")
class ParticipantCohortStatus {

    // Important: Keep fields in sync with ParticipantCohortStatusDao.ALL_COLUMNS_EXCEPT_REVIEW_ID.
    @get:EmbeddedId
    @get:AttributeOverrides(AttributeOverride(name = "cohortReviewId", column = Column(name = "cohort_review_id")), AttributeOverride(name = "participantId", column = Column(name = "participant_id")))
    var participantKey: ParticipantCohortStatusKey? = null
    @get:Column(name = "status")
    var status: Short? = null
    @get:Column(name = "gender_concept_id")
    var genderConceptId: Long? = null
    @get:Transient
    var gender: String? = null
    @get:Column(name = "birth_date")
    var birthDate: Date? = null
    @get:Column(name = "race_concept_id")
    var raceConceptId: Long? = null
    @get:Transient
    var race: String? = null
    @get:Column(name = "ethnicity_concept_id")
    var ethnicityConceptId: Long? = null
    @get:Transient
    var ethnicity: String? = null
    @get:Column(name = "deceased")
    var deceased: Boolean = false

    var statusEnum: CohortStatus
        @Transient
        get() = StorageEnums.cohortStatusFromStorage(status)
        set(status) {
            status = StorageEnums.cohortStatusToStorage(status)
        }

    fun participantKey(participantKey: ParticipantCohortStatusKey): ParticipantCohortStatus {
        this.participantKey = participantKey
        return this
    }

    fun status(status: Short?): ParticipantCohortStatus {
        this.status = status
        return this
    }

    fun statusEnum(status: CohortStatus): ParticipantCohortStatus {
        return this.status(StorageEnums.cohortStatusToStorage(status))
    }

    fun genderConceptId(genderConceptId: Long?): ParticipantCohortStatus {
        this.genderConceptId = genderConceptId
        return this
    }

    fun gender(gender: String): ParticipantCohortStatus {
        this.gender = gender
        return this
    }

    fun birthDate(birthDate: Date): ParticipantCohortStatus {
        this.birthDate = birthDate
        return this
    }

    fun raceConceptId(raceConceptId: Long?): ParticipantCohortStatus {
        this.raceConceptId = raceConceptId
        return this
    }

    fun race(race: String): ParticipantCohortStatus {
        this.race = race
        return this
    }

    fun ethnicityConceptId(ethnicityConceptId: Long?): ParticipantCohortStatus {
        this.ethnicityConceptId = ethnicityConceptId
        return this
    }

    fun ethnicity(ethnicity: String): ParticipantCohortStatus {
        this.ethnicity = ethnicity
        return this
    }

    fun deceased(deceased: Boolean): ParticipantCohortStatus {
        this.deceased = deceased
        return this
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as ParticipantCohortStatus?
        return (status === that!!.status
                && genderConceptId == that!!.genderConceptId
                && gender == that.gender
                && birthDate == that.birthDate
                && raceConceptId == that.raceConceptId
                && race == that.race
                && ethnicityConceptId == that.ethnicityConceptId
                && ethnicity == that.ethnicity)
    }

    override fun hashCode(): Int {
        return Objects.hash(
                status,
                genderConceptId,
                gender,
                birthDate,
                raceConceptId,
                race,
                ethnicityConceptId,
                ethnicity)
    }

    override fun toString(): String {
        return ToStringBuilder(this)
                .append("participantKey", participantKey)
                .append("status", status)
                .append("genderConceptId", genderConceptId)
                .append("gender", gender)
                .append("birthDate", birthDate)
                .append("raceConceptId", raceConceptId)
                .append("race", race)
                .append("ethnicityConceptId", ethnicityConceptId)
                .append("ethnicity", ethnicity)
                .toString()
    }
}
