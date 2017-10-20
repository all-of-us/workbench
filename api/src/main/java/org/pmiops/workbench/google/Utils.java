package org.pmiops.workbench.google;

import javax.servlet.ServletContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


public class Utils {

  public static ServletContext getRequestServletContext() {
    return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
      .getRequest().getServletContext();
  }
}
