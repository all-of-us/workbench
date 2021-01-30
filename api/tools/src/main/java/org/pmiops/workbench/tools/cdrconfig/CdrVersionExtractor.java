package org.pmiops.workbench.tools.cdrconfig;

import java.util.List;
import org.pmiops.workbench.db.model.DbCdrVersion;

// expected format:
// { "accessTiers": [ {DbAccessTier}, ...], "cdrVersions": [ {DbCdrVersion}, ...] }

// however we need to parse them separately because we use an adapter to resolve the accessTier
// field in the DbCdrVersion
class CdrVersionExtractor {
  public List<DbCdrVersion> cdrVersions;
}
