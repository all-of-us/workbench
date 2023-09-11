package org.pmiops.workbench.leonardo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.leonardo.model.LeonardoDiskType;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskStatus;

public final class PersistentDiskUtils {
  private static final Logger log = Logger.getLogger(PersistentDiskUtils.class.getName());

  // See https://cloud.google.com/compute/pricing
  private static final Map<LeonardoDiskType, Double> DISK_PRICE_PER_GB_MONTH =
      ImmutableMap.<LeonardoDiskType, Double>builder()
          .put(LeonardoDiskType.STANDARD, .04)
          .put(LeonardoDiskType.SSD, .17)
          .build();

  // https://github.com/DataBiosphere/leonardo/blob/3774547f2018e056e9af42142a10ac004cfe1ee8/core/src/main/scala/org/broadinstitute/dsde/workbench/leonardo/diskModels.scala#L60
  private static final Set<DiskStatus> ACTIVE_DISK_STATUSES =
      ImmutableSet.of(DiskStatus.READY, DiskStatus.CREATING, DiskStatus.RESTORING);

  private PersistentDiskUtils() {}

  // Keep in sync with ui/src/app/utils/machines.ts
  public static double costPerMonth(LeonardoListPersistentDiskResponse disk, String googleProject) {
    Double pricePerGbMonth = DISK_PRICE_PER_GB_MONTH.get(disk.getDiskType());
    if (pricePerGbMonth == null) {
      pricePerGbMonth = DISK_PRICE_PER_GB_MONTH.get(LeonardoDiskType.STANDARD);
      log.warning(
          String.format(
              "unknown disk type %s for disk %s/%s, defaulting to standard",
              disk.getDiskType(), googleProject, disk.getName()));
    }
    return pricePerGbMonth * disk.getSize();
  }

  /**
   * Finds the most recent disks for all apps and GCE runtime.
   *
   * <p>We use {@link Disk#getCreatedDate} as the most recent disk.
   */
  public static List<Disk> findTheMostRecentActiveDisks(List<Disk> disksToValidate) {
    // Iterate original list first to check if disks are valid. Print log if disks maybe in
    // incorrect state to help future debugging.
    // Disk maybe in incorrect state if having additional active state disks.

    List<Disk> activeDisks =
        disksToValidate.stream()
            .filter(d -> ACTIVE_DISK_STATUSES.contains(d.getStatus()))
            .collect(Collectors.toList());
    if (activeDisks.size() > (AppType.values().length + 1)) {
      String diskNameList =
          activeDisks.stream().map(Disk::getName).collect(Collectors.joining(","));
      log.warning(String.format("Maybe incorrect disks: %s", diskNameList));
    }

    List<Disk> recentDisks = new ArrayList<>();
    // Find the runtime disk with maximum creation time.
    Optional<Disk> runtimeDisk =
        activeDisks.stream()
            .filter(Disk::isIsGceRuntime)
            .max(Comparator.comparing((r) -> Instant.parse(r.getCreatedDate())));
    runtimeDisk.ifPresent(recentDisks::add);

    // For each app type, find the disk with maximum creation time.
    Map<AppType, Disk> appDisks =
        activeDisks.stream()
            .filter(d -> d.getAppType() != null)
            .collect(
                Collectors.toMap(
                    Disk::getAppType,
                    Function.identity(),
                    BinaryOperator.maxBy(
                        Comparator.comparing((r) -> Instant.parse(r.getCreatedDate())))));
    recentDisks.addAll(appDisks.values());
    return recentDisks;
  }

  /** Returns string of list of disk names, split by comma. */
  public static String prettyPrintDiskNames(List<Disk> disks) {
    return disks.stream().map(Disk::getName).map(String::toString).collect(Collectors.joining(","));
  }
}
