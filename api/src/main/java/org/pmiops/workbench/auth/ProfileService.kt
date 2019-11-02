package org.pmiops.workbench.auth

import java.math.BigDecimal
import java.util.ArrayList
import java.util.function.Function
import java.util.stream.Collectors
import org.pmiops.workbench.billing.FreeTierBillingService
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.model.DemographicSurvey
import org.pmiops.workbench.db.model.InstitutionalAffiliation
import org.pmiops.workbench.db.model.PageVisit
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.model.Address
import org.pmiops.workbench.model.DemographicSurvey
import org.pmiops.workbench.model.Disability
import org.pmiops.workbench.model.InstitutionalAffiliation
import org.pmiops.workbench.model.PageVisit
import org.pmiops.workbench.model.Profile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProfileService @Autowired
constructor(private val userDao: UserDao, private val freeTierBillingService: FreeTierBillingService) {

    fun getProfile(user: User): Profile {
        var user = user
        // Fetch the user's authorities, since they aren't loaded during normal request interception.
        val userWithAuthoritiesAndPageVisits = userDao.findUserWithAuthoritiesAndPageVisits(user.userId)
        if (userWithAuthoritiesAndPageVisits != null) {
            // If the user is already written to the database, use it and whatever authorities and page
            // visits are there.
            user = userWithAuthoritiesAndPageVisits
        }

        val profile = Profile()
        profile.setUserId(user.userId)
        profile.setUsername(user.email)
        if (user.creationNonce != null) {
            profile.setCreationNonce(user.creationNonce!!.toString())
        }
        profile.setFamilyName(user.familyName)
        profile.setGivenName(user.givenName)
        profile.setOrganization(user.organization)
        profile.setCurrentPosition(user.currentPosition)
        profile.setContactEmail(user.contactEmail)
        profile.setPhoneNumber(user.phoneNumber)
        profile.setAboutYou(user.aboutYou)
        profile.setAreaOfResearch(user.areaOfResearch)
        profile.setDisabled(user.disabled)
        profile.setEraCommonsLinkedNihUsername(user.eraCommonsLinkedNihUsername)

        if (user.complianceTrainingCompletionTime != null) {
            profile.setComplianceTrainingCompletionTime(
                    user.complianceTrainingCompletionTime!!.time)
        }
        if (user.complianceTrainingBypassTime != null) {
            profile.setComplianceTrainingBypassTime(user.complianceTrainingBypassTime!!.time)
        }
        if (user.eraCommonsLinkExpireTime != null) {
            profile.setEraCommonsLinkExpireTime(user.eraCommonsLinkExpireTime!!.time)
        }
        if (user.eraCommonsCompletionTime != null) {
            profile.setEraCommonsCompletionTime(user.eraCommonsCompletionTime!!.time)
        }
        if (user.eraCommonsBypassTime != null) {
            profile.setEraCommonsBypassTime(user.eraCommonsBypassTime!!.time)
        }
        if (user.demographicSurveyCompletionTime != null) {
            profile.setDemographicSurveyCompletionTime(
                    user.demographicSurveyCompletionTime!!.time)
        }
        if (user.firstSignInTime != null) {
            profile.setFirstSignInTime(user.firstSignInTime!!.time)
        }
        if (user.idVerificationBypassTime != null) {
            profile.setIdVerificationBypassTime(user.idVerificationBypassTime!!.time)
        }
        if (user.idVerificationCompletionTime != null) {
            profile.setIdVerificationCompletionTime(user.idVerificationCompletionTime!!.time)
        }
        if (user.dataAccessLevelEnum != null) {
            profile.setDataAccessLevel(user.dataAccessLevelEnum)
        }
        if (user.betaAccessBypassTime != null) {
            profile.setBetaAccessBypassTime(user.betaAccessBypassTime!!.time)
        }
        if (user.betaAccessRequestTime != null) {
            profile.setBetaAccessRequestTime(user.betaAccessRequestTime!!.time)
        }
        if (user.emailVerificationCompletionTime != null) {
            profile.setEmailVerificationCompletionTime(
                    user.emailVerificationCompletionTime!!.time)
        }
        if (user.emailVerificationBypassTime != null) {
            profile.setEmailVerificationBypassTime(user.emailVerificationBypassTime!!.time)
        }
        if (user.dataUseAgreementCompletionTime != null) {
            profile.setDataUseAgreementCompletionTime(user.dataUseAgreementCompletionTime!!.time)
        }
        if (user.dataUseAgreementBypassTime != null) {
            profile.setDataUseAgreementBypassTime(user.dataUseAgreementBypassTime!!.time)
        }
        if (user.dataUseAgreementSignedVersion != null) {
            profile.setDataUseAgreementSignedVersion(user.dataUseAgreementSignedVersion)
        }
        if (user.twoFactorAuthCompletionTime != null) {
            profile.setTwoFactorAuthCompletionTime(user.twoFactorAuthCompletionTime!!.time)
        }
        if (user.twoFactorAuthBypassTime != null) {
            profile.setTwoFactorAuthBypassTime(user.twoFactorAuthBypassTime!!.time)
        }
        if (user.authoritiesEnum != null) {
            profile.setAuthorities(ArrayList(user.authoritiesEnum!!))
        }
        if (user.pageVisits != null && !user.pageVisits.isEmpty()) {
            profile.setPageVisits(
                    user.pageVisits.stream().map(TO_CLIENT_PAGE_VISIT).collect(Collectors.toList<T>()))
        }
        if (user.demographicSurvey != null) {
            profile.setDemographicSurvey(TO_CLIENT_DEMOGRAPHIC_SURVEY.apply(user.demographicSurvey))
        }
        if (user.address != null) {
            profile.setAddress(TO_CLIENT_ADDRESS_SURVEY.apply(user.address))
        }
        profile.setInstitutionalAffiliations(
                user.institutionalAffiliations.stream()
                        .map(TO_CLIENT_INSTITUTIONAL_AFFILIATION)
                        .collect(Collectors.toList<T>()))
        profile.setEmailVerificationStatus(user.emailVerificationStatusEnum)

        profile.setFreeTierUsage(freeTierBillingService.getUserCachedFreeTierUsage(user))
        profile.setFreeTierQuota(freeTierBillingService.getUserFreeTierLimit(user))

        return profile
    }

    companion object {
        private val TO_CLIENT_INSTITUTIONAL_AFFILIATION = Function<org.pmiops.workbench.db.model.InstitutionalAffiliation, Any> { institutionalAffiliation ->
            val result = InstitutionalAffiliation()
            result.role = institutionalAffiliation.role
            result.institution = institutionalAffiliation.institution

            result
        }

        private val TO_CLIENT_PAGE_VISIT = Function<PageVisit, Any> { pageVisit ->
            val result = PageVisit()
            result.setPage(pageVisit.pageId)
            result.setFirstVisit(pageVisit.firstVisit!!.time)
            result
        }

        private val TO_CLIENT_DEMOGRAPHIC_SURVEY = Function<DemographicSurvey, Any> { demographicSurvey ->
            val result = DemographicSurvey()
            if (result.disability != null)
                result.disability = demographicSurvey.disabilityEnum.equals(Disability.TRUE)
            result.setEducation(demographicSurvey.educationEnum)
            result.setEthnicity(demographicSurvey.ethnicityEnum)
            result.setGender(demographicSurvey.genderEnum)
            result.setRace(demographicSurvey.raceEnum)
            result.setYearOfBirth(BigDecimal.valueOf(demographicSurvey.year_of_birth.toLong()))

            result
        }

        private val TO_CLIENT_ADDRESS_SURVEY = object : Function<org.pmiops.workbench.db.model.Address, Address> {
            override fun apply(address: org.pmiops.workbench.db.model.Address?): Address {
                val result = Address()
                if (address != null) {
                    result.setStreetAddress1(address.streetAddress1)
                    result.setStreetAddress2(address.streetAddress2)
                    result.setCity(address.city)
                    result.setState(address.state)
                    result.setCountry(address.country)
                    result.setZipCode(address.zipCode)
                    return result
                }
                return result
            }
        }
    }
}
