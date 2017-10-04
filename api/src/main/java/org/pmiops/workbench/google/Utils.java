package org.pmiops.workbench.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import org.pmiops.workbench.exceptions.ServerErrorException;
import java.io.IOException;

public class Utils {
  public static GoogleCredential getDefaultGoogleCredential() {
    try {
      return GoogleCredential.getApplicationDefault();
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
  }
}
