package org.pmiops.workbench.leonardo;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.pmiops.workbench.model.AppType;

/** Helper class for setting Leonardo labels. */
public class LeonardoLabelHelper {
  private LeonardoLabelHelper() {}

  public static final String LEONARDO_LABEL_AOU = "all-of-us";
  public static final String LEONARDO_LABEL_AOU_CONFIG = "all-of-us-config";
  public static final String LEONARDO_LABEL_CREATED_BY = "created-by";
  public static final String LEONARDO_LABEL_APP_TYPE = "aou-app-type";

  public static final String LEONARDO_LABEL_IS_RUNTIME = "is-runtime";
  public static final String LEONARDO_LABEL_IS_RUNTIME_TRUE = "true";

  public static final String LEONARDO_APP_LABEL_KEYS =
      LEONARDO_LABEL_APP_TYPE + "," + LEONARDO_LABEL_CREATED_BY;
  public static final String LEONARDO_DISK_LABEL_KEYS =
      LEONARDO_LABEL_APP_TYPE + "," + LEONARDO_LABEL_IS_RUNTIME;

  public static final String LEONARDO_LABEL_WORKSPACE_NAMESPACE = "saturnWorkspaceNamespace";
  public static final String LEONARDO_LABEL_WORKSPACE_NAME = "saturnWorkspaceName";

  // Important: keep these string constants in sync with LeonardoMapper
  // toConfigurationType() and toConfigurationLabel()

  public static String USER_OVERRIDE = "user-override";
  public static String GENERAL_ANALYSIS = "preset-general-analysis";
  public static String HAIL_GENOMIC_ANALYSIS = "preset-hail-genomic-analysis";

  public static boolean isValidRuntimeConfigurationLabel(String s) {
    return Set.of(USER_OVERRIDE, GENERAL_ANALYSIS, HAIL_GENOMIC_ANALYSIS).contains(s);
  }

  public static String appTypeToLabelValue(AppType appType) {
    return appType.toString().toLowerCase();
  }

  public static AppType labelValueToAppType(String labelValue) {
    return AppType.fromValue(labelValue.toUpperCase());
  }

  // a limitation in the Leo Swagger client code generation means that the labels come in as Object
  // rather than their true type of Map<String, String>
  @SuppressWarnings("unchecked")
  public static Map<String, String> toLabelMap(Object rawLabelObject) {
    return (Map<String, String>) rawLabelObject;
  }

  public static Optional<AppType> maybeMapLeonardoLabelsToGkeApp(@Nullable Object rawLabelObject) {
    return Optional.ofNullable(toLabelMap(rawLabelObject))
        .map(m -> m.get(LEONARDO_LABEL_APP_TYPE))
        .map(LeonardoLabelHelper::labelValueToAppType);
  }

  /** Insert or update disk labels. */
  public static Map<String, String> upsertLeonardoLabel(
      @Nullable Object rawLabelObject, String labelKey, String labelValue) {
    Map<String, String> labels =
        Optional.ofNullable(toLabelMap(rawLabelObject)).orElse(new HashMap<>());
    labels.put(labelKey, labelValue);
    return labels;
  }

  public static String getEnvironmentType(@Nullable Object labels) {
    return maybeMapLeonardoLabelsToGkeApp(labels)
        .map(LeonardoAppUtils::appDisplayName)
        .orElse("Jupyter");
  }
}
