package org.pmiops.workbench.actionaudit.targetproperties

import org.pmiops.workbench.model.Profile

// N.B. entries will rarely be referred to by name, but are accessed via values(). I.e. they are
// not safe to remove, even if an IDE indicates otherwise.
enum class ProfileTargetProperty
constructor(override val propertyName: String, override val extractor: (Profile) -> String?) : ModelBackedTargetProperty<Profile> {
    USER_NAME("user_name", Profile::getUsername),
    CONTACT_EMAIL("contact_email", Profile::getContactEmail),
    ACCESS_TIER_SHORT_NAMES("access_tier_short_names", { it.accessTierShortNames.joinToString(",") }),
    GIVEN_NAME("given_name", Profile::getGivenName),
    FAMILY_NAME("family_name", Profile::getFamilyName),
    DISABLED("disabled", { it.disabled?.toString() }),
    AREA_OF_RESEARCH("area_of_research", Profile::getAreaOfResearch),
    AFFILIATION("affiliation", { it.verifiedInstitutionalAffiliation?.toString() }),
    DEMOGRAPHIC_SURVEY("demographic_survey", { it.demographicSurvey?.toString() }),
    ADDRESS("address", { it.address?.toString() }),
}
