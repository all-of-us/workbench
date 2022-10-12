package org.pmiops.workbench.leonardo;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
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
}
