package org.pmiops.workbench.db.model

import java.sql.Timestamp
import java.util.Objects
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "user_data_use_agreement")
class UserDataUseAgreement {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "user_data_use_agreement_id")
    var userDataUseAgreementId: Long = 0
    @get:Column(name = "user_id")
    var userId: Long = 0
    @get:Column(name = "user_given_name")
    var userGivenName: String? = null
    @get:Column(name = "user_family_name")
    var userFamilyName: String? = null
    @get:Column(name = "user_initials")
    var userInitials: String? = null
    // This is set to 'true' whenever user_given_name or user_family_name are not the same as the
    // given_name or family_name on the User entry referenced by user_id.
    @get:Column(name = "user_name_out_of_date")
    var isUserNameOutOfDate: Boolean = false
    @get:Column(name = "data_use_agreement_signed_version")
    var dataUseAgreementSignedVersion: Int = 0
    @get:Column(name = "completion_time")
    var completionTime: Timestamp? = null

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val that = o as UserDataUseAgreement?
        return (userDataUseAgreementId == that!!.userDataUseAgreementId
                && userId == that.userId
                && isUserNameOutOfDate == that.isUserNameOutOfDate
                && dataUseAgreementSignedVersion == that.dataUseAgreementSignedVersion
                && userGivenName == that.userGivenName
                && userFamilyName == that.userFamilyName
                && userInitials == that.userInitials
                && completionTime!!.equals(that.completionTime))
    }

    override fun hashCode(): Int {
        return Objects.hash(
                userDataUseAgreementId,
                userId,
                userGivenName,
                userFamilyName,
                userInitials,
                isUserNameOutOfDate,
                dataUseAgreementSignedVersion,
                completionTime)
    }
}
