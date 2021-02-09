package org.pmiops.workbench.tools.cdrconfig;

import java.util.List;

// used by UpdateCdrConfig
// TODO: enable mapstruct in api/tools and move this class there

// a Value Object for JSON parsing of cdr_config_*.json
public class CdrConfigVO {
  public List<AccessTierVO> accessTiers;
  public List<CdrVersionVO> cdrVersions;
}
