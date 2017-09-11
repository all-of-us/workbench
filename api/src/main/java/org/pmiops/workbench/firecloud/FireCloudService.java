package org.pmiops.workbench.firecloud;

/**
 * Encapsulate Firecloud API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface FireCloudService {

  /**
   * @return true if the user making the current request is enabled in FireCloud, false otherwise.
   */
  boolean isRequesterEnabledInFirecloud() throws ApiException;

  /**
   * Registers the user in Firecloud.
   * @param contactEmail an email address that can be used to contact this user
   * @param firstName the user's first name
   * @param lastName the user's last name
   */
  void registerUser(String contactEmail, String firstName, String lastName) throws ApiException;

  /**
   * Creates a billing project owned by AllOfUs.
   */
  void createAllOfUsBillingProject(String projectName) throws ApiException;

  /**
   * Adds the specified user to the specified billing project.
   */
  void addUserToBillingProject(String email, String projectName) throws ApiException;

}
