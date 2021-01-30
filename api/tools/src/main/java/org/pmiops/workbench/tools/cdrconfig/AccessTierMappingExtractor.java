package org.pmiops.workbench.tools.cdrconfig;

import java.util.List;

// expected format:
// { "accessTiers": [ {DbAccessTier}, ...], "cdrVersions": [ {DbCdrVersion}, ...] }

// a tool for extracting cdrVersion -> accessTier mappings without referring to the DB
public class AccessTierMappingExtractor {
  public static class Inner {
    public long cdrVersionId;
    public long accessTier;
  }

  public List<Inner> cdrVersions;
}
