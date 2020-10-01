package org.pmiops.workbench.cdr.model;

public interface DbDomainCount {

  String getDomainId();

  long getCount();

  /**
   * Was unable to make this a boolean when using a native query. So the punt was to keep it a long
   * instead.
   */
  long getStandard();
}
