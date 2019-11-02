package org.pmiops.workbench.db.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.Transient
import org.pmiops.workbench.model.NonAcademicAffiliation

@Entity
@Table(name = "institutional_affiliation")
class InstitutionalAffiliation {

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "institutional_affiliation_id")
    var institutionalAffiliationId: Long = 0
    @get:ManyToOne
    @get:JoinColumn(name = "user_id")
    var user: User? = null
    @get:Column(name = "order_index")
    var orderIndex: Int = 0
    @get:Column(name = "institution")
    var institution: String? = null
    @get:Column(name = "role")
    var role: String? = null
    @get:Column(name = "non_academic_affiliation")
    var nonAcademicAffiliation: Short? = null
    @get:Column(name = "other")
    var other: String? = null

    val nonAcademicAffiliationEnum: NonAcademicAffiliation
        @Transient
        get() = DemographicSurveyEnum.nonAcademicAffiliationFromStorage(this.nonAcademicAffiliation)

    fun setNonAcademicAffiliationnEnum(affiliation: NonAcademicAffiliation) {
        this.nonAcademicAffiliation = DemographicSurveyEnum.nonAcademicAffiliationToStorage(affiliation)
    }
}
