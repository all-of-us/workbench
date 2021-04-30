package org.pmiops.workbench.tools.cdrconfig;

import java.sql.Timestamp;

// used by UpdateCdrConfig
// TODO: enable mapstruct in api/tools and move this class there

// a Value Object for JSON parsing of cdr_config_*.json
// adapted from DbCdrVersion
public class CdrVersionVO {
  public long cdrVersionId;
  public boolean isDefault;
  public String name;
  public String accessTier; // modified from DbAccessTier type in DbCdrVersion
  public short releaseNumber;
  public short archivalStatus;
  public String bigqueryProject;
  public String bigqueryDataset;
  public Timestamp creationTime;
  public int numParticipants;
  public String cdrDbName;
  public String elasticIndexBaseName;
  public String wgsBigqueryDataset;
  public Boolean hasFitbitData;
  public Boolean hasCopeSurveyData;
}
