package org.pmiops.workbench.vwb.common;

import autovalue.shaded.com.google.common.collect.ImmutableList;
import java.util.List;

/** Utilities for VWB API Clients */
public class VwbApiClientUtils {
  private VwbApiClientUtils() {}

  public static final List<String> SCOPES =
      ImmutableList.of(
          "https://www.googleapis.com/auth/userinfo.profile",
          "https://www.googleapis.com/auth/userinfo.email");
}
