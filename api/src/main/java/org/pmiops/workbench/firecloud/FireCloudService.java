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
}
