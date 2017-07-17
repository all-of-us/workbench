package org.pmiops.workbench.firecloud;

/**
 * Interface for accessing FireCloud APIs.
 */
public interface FireCloudService {

  /**
   * @return true if the user making the current request is enabled in FireCloud, false otherwise.
   */
  boolean isRequesterEnabledInFirecloud() throws ApiException;

}
