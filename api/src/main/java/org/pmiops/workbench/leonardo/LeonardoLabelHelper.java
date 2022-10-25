package org.pmiops.workbench.leonardo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.pmiops.workbench.model.AppType;

/** Helper class tp set Leonardo label keys and values */
public class LeonardoLabelHelper {
  private LeonardoLabelHelper() {}

  public static final String LEONARDO_LABEL_AOU = "all-of-us";
  public static final String LEONARDO_LABEL_AOU_CONFIG = "all-of-us-config";
  public static final String LEONARDO_LABEL_CREATED_BY = "created-by";
  public static final String LEONARDO_LABEL_APP_TYPE = "aou-app-type";

  public static final String LEONARDO_LABEL_IS_RUNTIME = "is-runtime";
  public static final String LEONARDO_LABEL_IS_RUNTIME_TRUE = "true";

  public static String appTypeToLabelValue(AppType appType) {
    return appType.toString().toLowerCase();
  }

  /** Insert of update disk labels. */
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
