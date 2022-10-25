package org.pmiops.workbench.leonardo;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.pmiops.workbench.leonardo.model.LeonardoDiskType;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;

public final class PersistentDiskUtils {
  private static final Logger log = Logger.getLogger(PersistentDiskUtils.class.getName());

  // See https://cloud.google.com/compute/pricing
  private static final Map<LeonardoDiskType, Double> DISK_PRICE_PER_GB_MONTH =
      ImmutableMap.<LeonardoDiskType, Double>builder()
          .put(LeonardoDiskType.STANDARD, .04)
          .put(LeonardoDiskType.SSD, .17)
          .build();

  private PersistentDiskUtils() {}

  public static final String PD_LABEL_RUNTIME_KEY = "runtime";
  public static final String PD_LABEL_RUNTIME_VALUE = "true";
  public static final String PD_LABEL_KEY_APP_TYPE = "appType";

  // Keep in sync with ui/src/app/utils/machines.ts
  public static double costPerMonth(LeonardoListPersistentDiskResponse disk) {
    Double pricePerGbMonth = DISK_PRICE_PER_GB_MONTH.get(disk.getDiskType());
    if (pricePerGbMonth == null) {
      pricePerGbMonth = DISK_PRICE_PER_GB_MONTH.get(LeonardoDiskType.STANDARD);
      log.warning(
          String.format(
              "unknown disk type %s for disk %s/%s, defaulting to standard",
              disk.getDiskType(), disk.getGoogleProject(), disk.getName()));
    }
    return pricePerGbMonth * disk.getSize();
  }

  @SuppressWarnings("unchecked")
  public static Map<String, String> upsertPdLabel(Object rawLabelObject, String labelKey, String labelValue) {
    Map<String, String> labels = (Map<String, String>) Optional.ofNullable(rawLabelObject).orElse(new HashMap<String, String>());
    labels.put(labelKey, labelValue);
    return labels;
  }
}
