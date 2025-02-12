package org.pmiops.workbench.tools.cdrconfig;

import java.sql.Timestamp;

// used by UpdateCdrConfig
// TODO: enable mapstruct in api/tools and move this class there

// used for JSON parsing of cdr_config_*.json
// adapted from DbCdrVersion
public record CdrVersionConfig(
    long cdrVersionId,
    Boolean isDefault,
    String name,
    // modified from DbAccessTier type in DbCdrVersion
    String accessTier,
    short archivalStatus,
    String bigqueryProject,
    String bigqueryDataset,
    Timestamp creationTime,
    int numParticipants,
    String cdrDbName,
    String wgsBigqueryDataset,
    String wgsFilterSetName,
    Boolean hasFitbitData,
    Boolean hasCopeSurveyData,
    Boolean hasFitbitSleepData,
    Boolean hasFitbitDeviceData,
    Boolean hasSurveyConductData,
    Boolean tanagraEnabled,
    String storageBasePath,
    String wgsVcfMergedStoragePath,
    String wgsHailStoragePath,
    String wgsCramManifestPath,
    String microarrayHailStoragePath,
    String microarrayVcfSingleSampleStoragePath,
    String microarrayVcfManifestPath,
    String microarrayIdatManifestPath,
    // 2023Q1 CDR Release
    String wgsVdsPath,
    String wgsExomeMultiHailPath,
    String wgsExomeSplitHailPath,
    String wgsExomeVcfPath,
    String wgsAcafThresholdMultiHailPath,
    String wgsAcafThresholdSplitHailPath,
    String wgsAcafThresholdVcfPath,
    String wgsClinvarMultiHailPath,
    String wgsClinvarSplitHailPath,
    String wgsClinvarVcfPath,
    String wgsLongReadsManifestPath,
    String wgsLongReadsHailGRCh38,
    String wgsLongReadsHailT2T,
    String wgsLongReadsJointVcfGRCh38,
    String wgsLongReadsJointVcfT2T,
    String vwbTemplateId,
    int publicReleaseNumber) {}
