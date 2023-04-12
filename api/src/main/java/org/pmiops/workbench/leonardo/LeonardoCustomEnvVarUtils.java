package org.pmiops.workbench.leonardo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

/** Utility class for holding Leonardo APP environment variables constant and methods. */
public class LeonardoCustomEnvVarUtils {

  private static final String MICROARRAY_HAIL_STORAGE_PATH_KEY = "MICROARRAY_HAIL_STORAGE_PATH";
  @VisibleForTesting public static final String WORKSPACE_CDR_ENV_KEY = "WORKSPACE_CDR";

  @VisibleForTesting
  public static String BIGQUERY_STORAGE_API_ENABLED_ENV_KEY = "BIGQUERY_STORAGE_API_ENABLED";

  private static final String WORKSPACE_NAMESPACE_KEY = "WORKSPACE_NAMESPACE";
  private static final String WORKSPACE_BUCKET_KEY = "WORKSPACE_BUCKET";
  private static final String JUPYTER_DEBUG_LOGGING_ENV_KEY = "JUPYTER_DEBUG_LOGGING";
  private static final String LEONARDO_BASE_URL = "LEONARDO_BASE_URL";
  private static final String CDR_STORAGE_PATH_KEY = "CDR_STORAGE_PATH";
  private static final String WGS_VCF_MERGED_STORAGE_PATH_KEY = "WGS_VCF_MERGED_STORAGE_PATH";
  private static final String WGS_HAIL_STORAGE_PATH_KEY = "WGS_HAIL_STORAGE_PATH";

  @VisibleForTesting
  public static final String WGS_CRAM_MANIFEST_PATH_KEY = "WGS_CRAM_MANIFEST_PATH";

  private static final String MICROARRAY_VCF_SINGLE_SAMPLE_STORAGE_PATH_KEY =
      "MICROARRAY_VCF_SINGLE_SAMPLE_STORAGE_PATH";
  private static final String MICROARRAY_VCF_MANIFEST_PATH_KEY = "MICROARRAY_VCF_MANIFEST_PATH";
  private static final String MICROARRAY_IDAT_MANIFEST_PATH_KEY = "MICROARRAY_IDAT_MANIFEST_PATH";

  /* CDR V7, Q12023 */
  private static final String WGS_VDS_STORAGE_PATH_KEY = "WGS_VDS_PATH";
  // Exome
  private static final String WGS_EXOME_MULTI_HAIL_PATH_KEY = "WGS_EXOME_MULTI_HAIL_PATH";
  private static final String WGS_EXOME_SPLIT_HAIL_PATH_KEY = "WGS_EXOME_SPLIT_HAIL_PATH";
  private static final String WGS_EXOME_VCF_PATH_KEY = "WGS_EXOME_VCF_PATH";
  // ACAF Threshold
  private static final String WGS_ACAF_THRESHOLD_MULTI_HAIL_PATH_KEY =
      "WGS_ACAF_THRESHOLD_MULTI_HAIL_PATH";
  private static final String WGS_ACAF_THRESHOLD_SPLIT_HAIL_PATH_KEY =
      "WGS_ACAF_THRESHOLD_SPLIT_HAIL_PATH";
  private static final String WGS_ACAF_THRESHOLD_VCF_PATH_KEY = "WGS_ACAF_THRESHOLD_VCF_PATH";

  // Clinvar
  private static final String WGS_CLINVAR_MULTI_HAIL_PATH_KEY = "WGS_CLINVAR_MULTI_HAIL_PATH";
  private static final String WGS_CLINVAR_SPLIT_HAIL_PATH_KEY = "WGS_CLINVAR_SPLIT_HAIL_PATH";
  private static final String WGS_CLINVAR_VCF_PATH_KEY = "WGS_CLINVAR_VCF_PATH";

  // Long reads
  private static final String LONG_READS_MANIFEST_PATH_KEY = "LONG_READS_MANIFEST_PATH";
  private static final String WGS_LONGREADS_HAIL_GRCH38_PATH_KEY = "WGS_LONGREADS_HAIL_GRCH38_PATH";
  private static final String WGS_LONGREADS_HAIL_T2T_PATH_KEY = "WGS_LONGREADS_HAIL_T2T_PATH";
  private static final String WGS_LONGREADS_JOINT_SNP_INDEL_VCF_GRCH38_PATH_KEY =
      "WGS_LONGREADS_JOINT_SNP_INDEL_VCF_GRCH38_PATH";
  private static final String WGS_LONGREADS_JOINT_SNP_INDEL_VCF_T2T_PATH_KEY =
      "WGS_LONGREADS_JOINT_SNP_INDEL_VCF_T2T_PATH";

  @VisibleForTesting
  public static final Map<String, String> FASTA_REFERENCE_ENV_VAR_MAP =
      new ImmutableMap.Builder<String, String>()
          .put(
              "HG38_REFERENCE_FASTA",
              "gs://genomics-public-data/references/hg38/v0/Homo_sapiens_assembly38.fasta")
          .put(
              "HG38_REFERENCE_FAI",
              "gs://genomics-public-data/references/hg38/v0/Homo_sapiens_assembly38.fasta.fai")
          .put(
              "HG38_REFERENCE_DICT",
              "gs://genomics-public-data/references/hg38/v0/Homo_sapiens_assembly38.dict")
          .build();

  public static Map<String, String> buildCdrEnvVars(DbCdrVersion cdrVersion) {
    Map<String, String> vars = new HashMap<>();
    vars.put(
        WORKSPACE_CDR_ENV_KEY,
        cdrVersion.getBigqueryProject() + "." + cdrVersion.getBigqueryDataset());

    String datasetsBucket = cdrVersion.getAccessTier().getDatasetsBucket();
    String bucketInfix = cdrVersion.getStorageBasePath();
    if (!Strings.isNullOrEmpty(datasetsBucket) && !Strings.isNullOrEmpty(bucketInfix)) {
      String basePath = joinStoragePaths(datasetsBucket, bucketInfix);
      Map<String, Optional<String>> partialStoragePaths =
          ImmutableMap.<String, Optional<String>>builder()
              .put(CDR_STORAGE_PATH_KEY, Optional.of("/"))
              .put(
                  WGS_VCF_MERGED_STORAGE_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsVcfMergedStoragePath()))
              .put(
                  WGS_HAIL_STORAGE_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsHailStoragePath()))
              .put(
                  WGS_CRAM_MANIFEST_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsCramManifestPath()))
              .put(
                  LeonardoCustomEnvVarUtils.MICROARRAY_HAIL_STORAGE_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getMicroarrayHailStoragePath()))
              .put(
                  MICROARRAY_VCF_SINGLE_SAMPLE_STORAGE_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getMicroarrayVcfSingleSampleStoragePath()))
              .put(
                  MICROARRAY_VCF_MANIFEST_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getMicroarrayVcfManifestPath()))
              .put(
                  MICROARRAY_IDAT_MANIFEST_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getMicroarrayIdatManifestPath()))
              .put(WGS_VDS_STORAGE_PATH_KEY, Optional.ofNullable(cdrVersion.getWgsVdsPath()))
              .put(
                  WGS_EXOME_MULTI_HAIL_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsExomeMultiHailPath()))
              .put(
                  WGS_EXOME_SPLIT_HAIL_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsExomeSplitHailPath()))
              .put(WGS_EXOME_VCF_PATH_KEY, Optional.ofNullable(cdrVersion.getWgsExomeVcfPath()))
              .put(
                  WGS_ACAF_THRESHOLD_MULTI_HAIL_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsAcafThresholdSplitHailPath()))
              .put(
                  WGS_ACAF_THRESHOLD_SPLIT_HAIL_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsAcafThresholdSplitHailPath()))
              .put(
                  WGS_ACAF_THRESHOLD_VCF_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsAcafThresholdVcfPath()))
              .put(
                  WGS_CLINVAR_MULTI_HAIL_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsClinvarSplitHailPath()))
              .put(
                  WGS_CLINVAR_SPLIT_HAIL_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsClinvarSplitHailPath()))
              .put(WGS_CLINVAR_VCF_PATH_KEY, Optional.ofNullable(cdrVersion.getWgsClinvarVcfPath()))
              .put(
                  LONG_READS_MANIFEST_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsLongReadsManifestPath()))
              .put(
                  WGS_LONGREADS_HAIL_GRCH38_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsLongReadsHailGRCh38()))
              .put(
                  WGS_LONGREADS_HAIL_T2T_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsLongReadsHailT2T()))
              .put(
                  WGS_LONGREADS_JOINT_SNP_INDEL_VCF_GRCH38_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsLongReadsJointVcfGRCh38()))
              .put(
                  WGS_LONGREADS_JOINT_SNP_INDEL_VCF_T2T_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsLongReadsJointVcfT2T()))
              .build();
      vars.putAll(
          partialStoragePaths.entrySet().stream()
              .filter(entry -> entry.getValue().filter(p -> !p.isEmpty()).isPresent())
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      entry -> joinStoragePaths(basePath, entry.getValue().get()))));
    }

    return vars;
  }

  /** The general environment variables that can be used in all Apps. */
  public static Map<String, String> getBaseEnvironmentVariables(
      DbWorkspace workspace, FireCloudService fireCloudService, WorkbenchConfig workbenchConfig) {
    Map<String, String> customEnvironmentVariables = new HashMap<>();
    FirecloudWorkspaceResponse fcWorkspaceResponse =
        fireCloudService
            .getWorkspace(workspace)
            .orElseThrow(() -> new NotFoundException("workspace not found"));
    customEnvironmentVariables.put(WORKSPACE_NAMESPACE_KEY, workspace.getWorkspaceNamespace());
    // This variable is already made available by Leonardo, but it's only exported in certain
    // notebooks contexts; this ensures it is always exported. See RW-7096.
    customEnvironmentVariables.put(
        WORKSPACE_BUCKET_KEY, "gs://" + fcWorkspaceResponse.getWorkspace().getBucketName());
    // In Terra V2 workspaces, all compute users have the bigquery.readSessionUser role per CA-1179.
    // In all workspaces, OWNERs have storage read session permission via the project viewer role.
    // If this variable is exported (with any value), codegen will use the BQ storage API, which is
    // ~200x faster for loading large dataframes from Bigquery.
    // After CA-952 is complete, this should always be exported.
    if (WorkspaceAccessLevel.OWNER.toString().equals(fcWorkspaceResponse.getAccessLevel())
        || workspace.isTerraV2Workspace()) {
      customEnvironmentVariables.put(BIGQUERY_STORAGE_API_ENABLED_ENV_KEY, "true");
    }
    customEnvironmentVariables.put(LEONARDO_BASE_URL, workbenchConfig.firecloud.leoBaseUrl);
    customEnvironmentVariables.putAll(buildCdrEnvVars(workspace.getCdrVersion()));

    customEnvironmentVariables.putAll(FASTA_REFERENCE_ENV_VAR_MAP);

    return customEnvironmentVariables;
  }

  private static String joinStoragePaths(String... paths) {
    final CharMatcher slashMatch = CharMatcher.is('/');
    return Arrays.stream(paths)
        .map(slashMatch::trimLeadingFrom)
        .map(slashMatch::trimTrailingFrom)
        .filter(p -> !p.isEmpty())
        .collect(Collectors.joining("/"));
  }

  private LeonardoCustomEnvVarUtils() {}
}
