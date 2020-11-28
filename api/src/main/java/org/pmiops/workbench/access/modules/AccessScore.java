package org.pmiops.workbench.access.modules;

/**
 * Possible scores or error conditions for access modules. Do not assume that the ordering is
 * semantic. New values may be appended later.
 */
public enum AccessScore {
  NOT_ATTEMPTED,
  FAILED,
  PENDING,
  PASSED,
  INVALID_ACCESS_MODULE;
}
