package org.pmiops.workbench.google;


import org.pmiops.workbench.model.FileDetail;

import java.util.List;

/**
 * Encapsulate Googe APIs for interfacing with Google Cloud Storage.
 */
public interface CloudStorageService {

  public String readInvitationKey();
  public String readBlockscoreApiKey();
  public List<FileDetail> getBucketFileList(String bucketName);
}
