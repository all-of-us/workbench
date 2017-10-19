package org.pmiops.workbench.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.IOException;
import javax.servlet.ServletContext;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


public class Utils {
  public static GoogleCredential getDefaultGoogleCredential() {
    try {
      return GoogleCredential.getApplicationDefault();
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
  }

  public static ServletContext getRequestServletContext() {
    return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
      .getRequest().getServletContext();
  }
}
