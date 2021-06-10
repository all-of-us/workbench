package org.pmiops.workbench.tools.cdrconfig;

// used by UpdateCdrConfig
// TODO: enable mapstruct in api/tools and move this class there

// a Value Object for JSON parsing of cdr_config_*.json
// adapted from DbAccessTier
public class AccessTierVO {
  public long accessTierId; // primary opaque key for DB use only
  public String shortName; // unique key exposed to API
  public String displayName;
  public String servicePerimeter;
  public String authDomainName;
  public String authDomainGroupEmail;
}
