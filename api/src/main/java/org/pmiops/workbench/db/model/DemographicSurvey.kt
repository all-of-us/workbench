package org.pmiops.workbench.db.model

import java.util.stream.Collectors
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.Transient
import org.pmiops.workbench.model.Disability
import org.pmiops.workbench.model.Education
import org.pmiops.workbench.model.Ethnicity
import org.pmiops.workbench.model.Gender
import org.pmiops.workbench.model.Race

@Entity
@Table(name = "demographic_survey")
class DemographicSurvey {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "demographic_survey_id")
    var id: Long = 0
    @get:ElementCollection(fetch = FetchType.LAZY)
    @get:CollectionTable(name = "demographic_survey_race", joinColumns = [JoinColumn(name = "demographic_survey_id")])
    @get:Column(name = "race")
    var race: List<Short>? = null
    @get:Column(name = "ethnicity")
    var ethnicity: Short? = null
    @get:ElementCollection(fetch = FetchType.LAZY)
    @get:CollectionTable(name = "demographic_survey_gender", joinColumns = [JoinColumn(name = "demographic_survey_id")])
    @get:Column(name = "gender")
    var gender: List<Short>? = null
    @get:Column(name = "year_of_birth")
    var year_of_birth: Int = 0
    @get:Column(name = "education")
    var education: Short? = null
    @get:Column(name = "disability")
    var disability: Short? = null
    @get:ManyToOne
    @get:JoinColumn(name = "user_id")
    var user: User? = null

    var raceEnum: List<Race>?
        @Transient
        get() = if (race == null) null else this.race!!.stream()
                .map<Any> { raceObject -> DemographicSurveyEnum.raceFromStorage(raceObject) }
                .collect<List<Race>, Any>(Collectors.toList())
        set(raceList) {
            this.race = raceList.stream()
                    .map { race -> DemographicSurveyEnum.raceToStorage(race) }
                    .collect<List<Short>, Any>(Collectors.toList())
        }

    var ethnicityEnum: Ethnicity?
        @Transient
        get() = if (ethnicity == null) null else DemographicSurveyEnum.ethnicityFromStorage(ethnicity)
        set(ethnicity) {
            this.ethnicity = DemographicSurveyEnum.ethnicityToStorage(ethnicity)
        }

    var genderEnum: List<Gender>?
        @Transient
        get() = if (gender == null) null else this.gender!!.stream()
                .map<Any> { gender -> DemographicSurveyEnum.genderFromStorage(gender) }
                .collect<List<Gender>, Any>(Collectors.toList())
        set(genderList) {
            this.gender = genderList.stream()
                    .map { gender -> DemographicSurveyEnum.genderToStorage(gender) }
                    .collect<List<Short>, Any>(Collectors.toList())
        }

    var educationEnum: Education?
        @Transient
        get() = if (education == null) null else DemographicSurveyEnum.educationFromStorage(education)
        set(education) {
            this.education = DemographicSurveyEnum.educationToStorage(education)
        }

    var disabilityEnum: Disability
        @Transient
        get() = DemographicSurveyEnum.disabilityFromStorage(disability)
        set(disability) {
            this.disability = DemographicSurveyEnum.disabilityToStorage(disability)
        }

    constructor() {}

    constructor(demographicSurvey: org.pmiops.workbench.model.DemographicSurvey) {
        this.race = demographicSurvey.getRace().stream()
                .map({ race -> DemographicSurveyEnum.raceToStorage(race) })
                .collect(Collectors.toList<T>())
        this.ethnicity = DemographicSurveyEnum.ethnicityToStorage(demographicSurvey.getEthnicity())
        this.gender = demographicSurvey.getGender().stream()
                .map({ gender -> DemographicSurveyEnum.genderToStorage(gender) })
                .collect(Collectors.toList<T>())
        this.year_of_birth = demographicSurvey.getYearOfBirth().intValue()
        this.education = DemographicSurveyEnum.educationToStorage(demographicSurvey.getEducation())
        this.disability = DemographicSurveyEnum.disabilityToStorage(demographicSurvey.getDisability())
    }
}
