package org.pmiops.workbench.tools.cdrconfig;

import java.util.List;

// used by UpdateCdrConfig
// TODO: enable mapstruct in api/tools and move this class there

// used for JSON parsing of cdr_config_*.json
public record CdrConfigRecord(List<AccessTierConfig> accessTiers, List<CdrVersionVO> cdrVersions) {}
