package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.model.PublicInstitutionDetails;

public interface InstitutionService {
  List<Institution> getInstitutions();

  List<PublicInstitutionDetails> getPublicInstitutionDetails();

  Optional<Institution> getInstitution(final String shortName);

  // throws NotFoundException if the DbInstitution is not found
  DbInstitution getDbInstitutionOrThrow(final String shortName);

  Institution createInstitution(final Institution institutionToCreate);

  void deleteInstitution(final String shortName);

  Optional<Institution> updateInstitution(
      final String shortName, final Institution institutionToUpdate);

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
   * @return boolean – is the contact email a valid member
   */
  boolean validateInstitutionalEmail(Institution institution, String contactEmail);

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
}
