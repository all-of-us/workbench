package org.pmiops.workbench.audit.targetproperties

import org.pmiops.workbench.model.Profile

enum class ProfileTargetProperty(val propertyName: String, private val extractor: (Profile) -> String?) {
    USER_NAME("user_name", Profile::getUsername),
    CREATION_NONCE("creation_nonce", Profile::getCreationNonce),
    CONTACT_EMAIL("contact_email", Profile::getContactEmail),
    DATA_ACCESS_LEVEL("data_access_level", { it.dataAccessLevel.toString() }),
    GIVEN_NAME("given_name", Profile::getGivenName),
    FAMILY_NAME("family_name", Profile::getFamilyName),
    PHONE_NUMBER("phone_number", Profile::getPhoneNumber),
    CURRENT_POSITION("current_position", Profile::getCurrentPosition),
    ORGANIZATION("organization", Profile::getOrganization),
    DISABLED("disabled", { it.disabled.toString() }),
    ABOUT_YOU("about_you", Profile::getAboutYou),
    AREA_OF_RESEARCH("area_of_research", Profile::getAreaOfResearch),
    INSTITUTIONAL_AFFILIATIONS("institutional_affiliations",
            { it.institutionalAffiliations.joinToString(", ") }),
    DEMOGRAPHIC_SURVEY("demographic_survey", { it.demographicSurvey.toString() }),
    ADDRESS("address", { it.address?.toString() }),

    fun extract(profile: Profile): String? {
        return extractor.invoke(profile)
    }

    companion object {
        @JvmStatic
        fun getPropertyValuesByName(profile: Profile): Map<String, String> {
            return values()
                    .filter { it.extract(profile) != null }
                    .map { it.propertyName to it.extract(profile)!! }
                    .toMap()
        }

        @JvmStatic
        fun getChangedValuesByName(
                previousProfile: Profile,
                newProfile: Profile
        ): Map<String, PreviousNewValuePair> {
            return values()
                    .map { it.propertyName to
                            PreviousNewValuePair(
                                    previousValue = it.extract(previousProfile),
                                    newValue = it.extract(newProfile)) }
                    .filter { it.second.valueChanged }
                    .toMap()
        }
    }
}
