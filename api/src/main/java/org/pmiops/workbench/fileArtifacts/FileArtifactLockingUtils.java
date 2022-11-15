package org.pmiops.workbench.fileArtifacts;

import com.google.common.io.BaseEncoding;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import org.apache.arrow.util.VisibleForTesting;
import org.pmiops.workbench.exceptions.ServerErrorException;

public class FileArtifactLockingUtils {
  @VisibleForTesting
  public static String notebookLockingEmailHash(String bucket, String email) {
    String toHash = String.format("%s:%s", bucket, email);
    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      byte[] hash = sha256.digest(toHash.getBytes(StandardCharsets.UTF_8));
      // convert to printable hex text
      return BaseEncoding.base16().lowerCase().encode(hash);
    } catch (final NoSuchAlgorithmException e) {
      throw new ServerErrorException(e);
    }
  }

  public static String findHashedUser(String bucket, Set<String> workspaceUsers, String hash) {
    return workspaceUsers.stream()
        .filter(email -> notebookLockingEmailHash(bucket, email).equals(hash))
        .findAny()
        .orElse("UNKNOWN");
  }
}
