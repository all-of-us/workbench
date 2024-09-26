package org.pmiops.workbench.institution;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionTierConfig;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.model.PublicInstitutionDetails;
import org.pmiops.workbench.model.UserTierEligibility;

public interface InstitutionService {
  List<Institution> getInstitutions();

  List<PublicInstitutionDetails> getPublicInstitutionDetails();

  Optional<Institution> getInstitution(final String shortName);

  // throws NotFoundException if the DbInstitution is not found
  DbInstitution getDbInstitutionOrThrow(final String shortName);

  Institution createInstitution(final Institution institutionToCreate);

  void deleteInstitution(final String shortName);

  /**
   * Update an institution in the DB if one matches the shortName.
   *
   * @param shortName
   * @param updatedInstitution
   * @return the updated institution, if an institution with the short name existed and the
   *     operation was successful. Empty if there was no matching institution to update.
   */
  Optional<Institution> updateInstitution(
      final String shortName, final Institution updatedInstitution);

  /**
   * Checks that the user's institutional affiliation is valid by calling
   * validateInstitutionalEmail. See Javadoc below for implementation notes.
   *
   * <p>If dbAffiliation is null, this method will return false.
   *
   * @param dbAffiliation the user's declared affiliation
   * @param contactEmail the contact email to validate
   * @return boolean - does the affiliation pass validation?
   */
  boolean validateAffiliation(
      @Nullable DbVerifiedInstitutionalAffiliation dbAffiliation, String contactEmail);

  /**
   * Checks whether a given email address is a valid member of an Institution.
   *
   * <p>Validation is done by comparing the contact email to the institution's set of whitelisted
   * email domains and addresses. An exact match on either the domain or the email address is
   * required.
   *
   * @param institution the institution to validate against
   * @param contactEmail contact email to validate
   * @param accessTierShortName the name of the access tier to verify.
   * @return boolean – is the contact email a valid member
   */
  boolean validateInstitutionalEmail(
      Institution institution, String contactEmail, String accessTierShortName);

  /**
   * Retrieve the optional text block of user instructions to fill the instructions email sent after
   * a user in this institution creates an account. Throws NotFoundException if the Institution does
   * not exist.
   *
   * @param shortName the short name (key) used to refer to this institution in the API
   * @return The text block of user instructions, or Empty if it is not present.
   */
  Optional<String> getInstitutionUserInstructions(final String shortName);

  /**
   * Create or update the text block of user instructions to be included in the instructions email
   * sent after a user in this institution creates an account. Sanitizes inputs to remove all HTML
   * tags. Throws NotFoundException if the Institution does not exist.
   *
   * @param instructions The institution to update along with the text to include in the email
   */
  void setInstitutionUserInstructions(final InstitutionUserInstructions instructions);

  /**
   * Delete the text block of user instructions to be included in the instructions email for this
   * institution, if it exists. Throws NotFoundException if the Institution does not exist.
   *
   * @param shortName the short name (key) used to refer to this institution in the API
   * @return whether user instructions were deleted
   */
  boolean deleteInstitutionUserInstructions(final String shortName);

  /**
   * Validates if the institution is for operational User
   *
   * @param institution
   * @return
   */
  boolean validateOperationalUser(DbInstitution institution);

  /**
   * Searches through all institutions and returns the first institution that matches the given
   * contact email address, if any.
   *
   * @param contactEmail
   * @return
   */
  Optional<Institution> getFirstMatchingInstitution(final String contactEmail);

  Optional<Institution> getByUser(DbUser user);

  List<DbUser> getAffiliatedUsers(String shortName);

  /**
   * Retrieve list of the {@link InstitutionTierConfig} which specifies how members can access each
   * tier and the requirement for each tier.
   *
   * @param institutionShortName the short name (key) used to refer to this institution in the API
   * @return the list of {@link InstitutionTierConfig} for each tier.
   */
  List<InstitutionTierConfig> getTierConfigs(String institutionShortName);
  /**
   * Returns the access tiers that the user's institution allow the user to join.
   *
   * @return
   */
  List<UserTierEligibility> getUserTierEligibilities(DbUser user);

  /** If true, this institution's users are exempt from credit expiration. */
  boolean shouldBypassForCreditsExpiration(DbUser user);
}
