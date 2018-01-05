package org.pmiops.workbench.google;

import java.util.Map;

/**
 * Encapsulate Googe APIs for interfacing with Google Cloud Storage.
 */
public interface CloudStorageService {

  public String readInvitationKey();
  public String readBlockscoreApiKey();
  public void writeFile(String bucketName, String fileName, byte[] bytes);
}
