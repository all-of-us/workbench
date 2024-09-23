package org.pmiops.workbench.interceptors;

import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class BlockAllTrafficInterceptor implements HandlerInterceptor {

  @Autowired Provider<WorkbenchConfig> workbenchConfigProvider;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    // Blocks all traffic if isDownForMaintenance is true
    return !workbenchConfigProvider.get().server.isDownForMaintenance;
  }
}
