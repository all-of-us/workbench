package org.pmiops.workbench.actionaudit.targetproperties

import org.pmiops.workbench.model.Profile

// N.B. entries will rarely be referred to by name, but are accessed via values(). I.e. they are
// not safe to remove, even if an IDE indicates otherwise.
enum class ProfileTargetProperty
constructor(override val propertyName: String, override val extractor: (Profile) -> String?) : ModelBackedTargetProperty<Profile> {
    USER_NAME("user_name", Profile::getUsername),
    CONTACT_EMAIL("contact_email", Profile::getContactEmail),
    DATA_ACCESS_LEVEL("data_access_level", { it.dataAccessLevel?.toString() }),
    GIVEN_NAME("given_name", Profile::getGivenName),
    FAMILY_NAME("family_name", Profile::getFamilyName),
    PHONE_NUMBER("phone_number", Profile::getPhoneNumber),
    CURRENT_POSITION("current_position", Profile::getCurrentPosition),
    ORGANIZATION("organization", Profile::getOrganization),
    DISABLED("disabled", { it.disabled?.toString() }),
    ABOUT_YOU("about_you", Profile::getAboutYou),
    AREA_OF_RESEARCH("area_of_research", Profile::getAreaOfResearch),
    DEPRECATED_INSTITUTIONAL_AFFILIATIONS("deprecated_institutional_affiliations",
            { it.deprecatedInstitutionalAffiliations?.joinToString(", ") }),
    DEMOGRAPHIC_SURVEY("demographic_survey", { it.demographicSurvey?.toString() }),
    ADDRESS("address", { it.address?.toString() });
}
