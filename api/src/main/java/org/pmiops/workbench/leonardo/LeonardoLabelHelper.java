package org.pmiops.workbench.leonardo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
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

  public static String appTypeToLabelValue(AppType appType) {
    return appType.toString().toLowerCase();
  }

  public static AppType labelValueToAppType(String labelValue) {
    return AppType.fromValue(labelValue.toUpperCase());
  }

  // a limitation in the Leo Swagger client code generation means that the labels come in as Object
  // rather than their true type of Map<String, String>
  @SuppressWarnings("unchecked")
  public static Optional<AppType> maybeMapLeonardoLabelsToGkeApp(@Nullable Object diskLabels) {
    return Optional.ofNullable((Map<String, String>) diskLabels)
        .flatMap(m -> Optional.ofNullable(m.get(LEONARDO_LABEL_APP_TYPE)))
        .map(LeonardoLabelHelper::labelValueToAppType);
  }

  /** Insert or update disk labels. */
  @SuppressWarnings("unchecked")
  public static Map<String, String> upsertLeonardoLabel(
      Object rawLabelObject, String labelKey, String labelValue) {
    Map<String, String> labels =
        (Map<String, String>)
            Optional.ofNullable(rawLabelObject).orElse(new HashMap<String, String>());
    labels.put(labelKey, labelValue);
    return labels;
  }
}
