package org.pmiops.workbench.billing;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.BillingConfig;
import org.pmiops.workbench.db.dao.BillingProjectBufferEntryDao;
import org.pmiops.workbench.db.dao.BillingProjectGarbageCollectionDao;
import org.pmiops.workbench.db.model.BillingProjectBufferEntry;
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus;
import org.pmiops.workbench.db.model.BillingProjectGarbageCollection;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.BillingProjectGarbageCollectionResponse;
import org.pmiops.workbench.model.GarbageCollectedProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingGarbageCollectionService {
  private final BillingProjectGarbageCollectionDao billingProjectGarbageCollectionDao;
  private final BillingProjectBufferEntryDao billingProjectBufferEntryDao;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<CloudStorageService> cloudStorageServiceProvider;
  private final Clock clock;
  private final LoadingCache<String, GoogleCredential> garbageCollectionSACredentials;

  @Autowired
  public BillingGarbageCollectionService(
      BillingProjectGarbageCollectionDao billingProjectGarbageCollectionDao,
      BillingProjectBufferEntryDao billingProjectBufferEntryDao,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<CloudStorageService> cloudStorageServiceProvider,
      Clock clock) {
    this.billingProjectGarbageCollectionDao = billingProjectGarbageCollectionDao;
    this.billingProjectBufferEntryDao = billingProjectBufferEntryDao;
    this.fireCloudService = fireCloudService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.cloudStorageServiceProvider = cloudStorageServiceProvider;
    this.clock = clock;

    this.garbageCollectionSACredentials = initializeCredentialCache();
  }

  private LoadingCache<String, GoogleCredential> initializeCredentialCache() {
    return CacheBuilder.newBuilder()
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build(
            new CacheLoader<String, GoogleCredential>() {
              @Override
              public GoogleCredential load(String saEmail) throws IOException {
                return cloudStorageServiceProvider
                    .get()
                    .getGarbageCollectionServiceAccountCredentials(saEmail)
                    .createScoped(FireCloudServiceImpl.FIRECLOUD_API_OAUTH_SCOPES);
              }
            });
  }

  // Is the App Engine SA a member of this FireCloud project?
  private boolean appSAIsMemberOfProject(String billingProject) {
    try {
      fireCloudService.getBillingProjectStatus(billingProject);
      return true;
    } catch (NotFoundException e) {
      return false;
    }
  }

  private String chooseGarbageCollectionSA() {
    final BillingConfig config = workbenchConfigProvider.get().billing;
    for (final String sa : config.garbageCollectionUsers) {
      final long count = billingProjectGarbageCollectionDao.countAllByOwner(sa);
      if (count < config.garbageCollectionUserCapacity) {
        return sa;
      }
    }

    final String msg =
        String.format(
            "No available Garbage Collection Service Accounts.  "
                + "These GCSAs exceed the configured capacity limit of %d: %s",
            config.garbageCollectionUserCapacity, String.join(", ", config.garbageCollectionUsers));
    throw new ServerErrorException(msg);
  }

  private GarbageCollectedProject recordGarbageCollection(
      final String projectName, final String garbageCollectionSA) {

    final BillingProjectGarbageCollection gc = new BillingProjectGarbageCollection();
    gc.setFireCloudProjectName(projectName);
    gc.setOwner(garbageCollectionSA);
    billingProjectGarbageCollectionDao.save(gc);

    final BillingProjectBufferEntry entry =
        billingProjectBufferEntryDao.findByFireCloudProjectName(projectName);
    entry.setStatusEnum(
        BillingProjectBufferStatus.GARBAGE_COLLECTED,
        () -> new Timestamp(clock.instant().toEpochMilli()));
    billingProjectBufferEntryDao.save(entry);

    return gc.toGarbageCollectedProject();
  }

  private GarbageCollectedProject transferOwnership(final String projectName) {
    final String appEngineSA = workbenchConfigProvider.get().auth.serviceAccountApiUsers.get(0);

    final String garbageCollectionSA = chooseGarbageCollectionSA();
    fireCloudService.addOwnerToBillingProject(garbageCollectionSA, projectName);

    try {
      final GoogleCredential gcsaCredential =
          garbageCollectionSACredentials.get(garbageCollectionSA);
      fireCloudService.removeOwnerFromBillingProject(appEngineSA, gcsaCredential, projectName);
    } catch (final ExecutionException e) {
      final String msg =
          String.format(
              "Failure retrieving credentials for garbage collection service account %s",
              garbageCollectionSA);
      throw new ServerErrorException(msg, e);
    } catch (final IOException e) {
      final String msg =
          String.format(
              "Failure removing user %s as owner of project %s. Successfully added new owner %s",
              appEngineSA, projectName, garbageCollectionSA);
      throw new ServerErrorException(msg, e);
    }

    return recordGarbageCollection(projectName, garbageCollectionSA);
  }

  BillingProjectGarbageCollectionResponse deletedWorkspaceGarbageCollection() {
    final List<String> billingProjects =
        billingProjectGarbageCollectionDao.findBillingProjectsForGarbageCollection();

    final List<GarbageCollectedProject> garbageCollectedProjects =
        billingProjects.stream()
            .map(
                projectName -> {
                  // determine whether this candidate for garbage collection
                  // has already been deleted or transferred by a process other than GC
                  if (appSAIsMemberOfProject(projectName)) {
                    return transferOwnership(projectName);
                  } else {
                    // if it's in the DB as garbage-collected we won't try to do it again
                    return recordGarbageCollection(projectName, "unknown");
                  }
                })
            .collect(Collectors.toList());

    final BillingProjectGarbageCollectionResponse response =
        new BillingProjectGarbageCollectionResponse();
    response.addAll(garbageCollectedProjects);
    return response;
  }
}
