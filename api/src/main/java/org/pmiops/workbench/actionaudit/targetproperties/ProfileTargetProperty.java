package org.pmiops.workbench.actionaudit.targetproperties;

import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.model.Profile;

// N.B. entries will rarely be referred to by name, but are accessed via values(). I.e. they are
// not safe to remove, even if an IDE indicates otherwise.
public enum ProfileTargetProperty implements ModelBackedTargetProperty<Profile> {
  USER_NAME("user_name", Profile::getUsername),
  CONTACT_EMAIL("contact_email", Profile::getContactEmail),
  ACCESS_TIER_SHORT_NAMES(
      "access_tier_short_names", profile -> String.join(",", profile.getAccessTierShortNames())),
  GIVEN_NAME("given_name", Profile::getGivenName),
  FAMILY_NAME("family_name", Profile::getFamilyName),
  DISABLED("disabled", profile -> PropertyUtils.toStringOrNull(profile.isDisabled())),
  AREA_OF_RESEARCH("area_of_research", Profile::getAreaOfResearch),
  AFFILIATION(
      "affiliation",
      profile -> PropertyUtils.toStringOrNull(profile.getVerifiedInstitutionalAffiliation())),
  DEMOGRAPHIC_SURVEY(
      "demographic_survey_is_present",
      profile -> PropertyUtils.toStringOrNull(profile.getDemographicSurvey())),
  ADDRESS("address_is_present", PropertyUtils.stringOrNullExtractor(Profile::getAddress));

  private final String propertyName;
  private final Function<Profile, String> extractor;

  ProfileTargetProperty(String propertyName, Function<Profile, String> extractor) {
    this.propertyName = propertyName;
    this.extractor = extractor;
  }

  @NotNull
  @Override
  public String getPropertyName() {
    return propertyName;
  }

  @NotNull
  @Override
  public Function<Profile, String> getExtractor() {
    return extractor;
  }
}
