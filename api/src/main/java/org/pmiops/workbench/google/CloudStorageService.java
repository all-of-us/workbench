package org.pmiops.workbench.google;


import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.BlobDetail;

import java.util.List;

/**
 * Encapsulate Googe APIs for interfacing with Google Cloud Storage.
 */
public interface CloudStorageService {

  public String readInvitationKey();
  public String readBlockscoreApiKey();
  public List<BlobDetail> getBucketFileList(String bucketName, String directory);
  public void writeFile(String bucketName, String fileName, byte[] bytes);
}
