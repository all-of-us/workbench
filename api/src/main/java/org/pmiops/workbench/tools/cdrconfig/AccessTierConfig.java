package org.pmiops.workbench.tools.cdrconfig;

// used by UpdateCdrConfig
// TODO: enable mapstruct in api/tools and move this class there

// used for JSON parsing of cdr_config_*.json
// adapted from DbAccessTier
public record AccessTierConfig(
    // primary opaque key for DB use only
    long accessTierId,
    // unique key exposed to the API
    String shortName,
    String displayName,
    String servicePerimeter,
    String authDomainName,
    String authDomainGroupEmail,
    String datasetsBucket,
    Boolean enableUserWorkflows,
    String vwbTierGroupName) {}
