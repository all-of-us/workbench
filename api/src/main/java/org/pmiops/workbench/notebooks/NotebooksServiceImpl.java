package org.pmiops.workbench.notebooks;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.json.JSONException;
import org.json.JSONObject;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.GoogleCloudLocators;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.monitoring.LogsBasedMetricService;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotebooksServiceImpl implements NotebooksService {

  private static final Pattern NOTEBOOK_PATTERN =
      Pattern.compile(NOTEBOOKS_WORKSPACE_DIRECTORY + "/[^/]+(\\.(?i)(ipynb))$");
  // Experimentally determined that generating the preview HTML for a >11MB notebook results in
  // OOMs on a default F1 240MB GAE task. OOMs may still occur during concurrent requests. If this
  // issue persists, we can move preview processing onto the client (calling Calhoun), or fully
  // client-side (using a client-side notebook renderer).
  private static final long MAX_NOTEBOOK_READ_SIZE_BYTES = 5 * 1000 * 1000; // 5MB
  private static final PolicyFactory PREVIEW_SANITIZER =
      Sanitizers.FORMATTING
          .and(Sanitizers.BLOCKS)
          .and(Sanitizers.LINKS)
          .and(Sanitizers.STYLES)
          .and(Sanitizers.TABLES)
          .and(
              new HtmlPolicyBuilder()
                  .allowElements("img")
                  .allowUrlProtocols("data")
                  .allowAttributes("src")
                  .matching(Pattern.compile("^data:image.*"))
                  .onElements("img")
                  .toFactory())
          .and(
              new HtmlPolicyBuilder()
                  // nbconvert renders styles into a style tag; unfortunately the OWASP library does
                  // not provide a good way of sanitizing this. This may render our iframe
                  // vulnerable to injection if vulnerabilities in nbconvert allow for custom style
                  // tag injection.
                  .allowTextIn("style")
                  // <pre> is not included in the prebuilt sanitizers; it is used for monospace code
                  // block formatting
                  .allowElements("style", "pre")

                  // Allow id/class in order to interact with the style tag.
                  .allowAttributes("id", "class")
                  .globally()
                  .toFactory());

  private final Clock clock;
  private final CloudStorageClient cloudStorageClient;
  private final FireCloudService fireCloudService;
  private final Provider<DbUser> userProvider;
  private final UserRecentResourceService userRecentResourceService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceAuthService workspaceAuthService;
  private final LogsBasedMetricService logsBasedMetricService;

  @Autowired
  public NotebooksServiceImpl(
      Clock clock,
      CloudStorageClient cloudStorageClient,
      FireCloudService fireCloudService,
      Provider<DbUser> userProvider,
      UserRecentResourceService userRecentResourceService,
      WorkspaceDao workspaceDao,
      WorkspaceAuthService workspaceAuthService,
      LogsBasedMetricService logsBasedMetricService) {
    this.clock = clock;
    this.cloudStorageClient = cloudStorageClient;
    this.fireCloudService = fireCloudService;
    this.userProvider = userProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.workspaceDao = workspaceDao;
    this.workspaceAuthService = workspaceAuthService;
    this.logsBasedMetricService = logsBasedMetricService;
  }

  // NOTE: may be an undercount since we only retrieve the first Page of Storage List results
  @Override
  public List<FileDetail> getNotebooks(String workspaceNamespace, String workspaceName) {
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();
    return getNotebooksAsService(bucketName);
  }

  // NOTE: may be an undercount since we only retrieve the first Page of Storage List results
  @Override
  public List<FileDetail> getNotebooksAsService(String bucketName) {
    return cloudStorageClient
        .getBlobPageForPrefix(bucketName, NOTEBOOKS_WORKSPACE_DIRECTORY)
        .stream()
        .filter(this::isNotebookBlob)
        .map(blob -> cloudStorageClient.blobToFileDetail(blob, bucketName))
        .collect(Collectors.toList());
  }

  @Override
  public boolean isNotebookBlob(Blob blob) {
    return NOTEBOOK_PATTERN.matcher(blob.getName()).matches();
  }

  @Override
  public FileDetail copyNotebook(
      String fromWorkspaceNamespace,
      String fromWorkspaceFirecloudName,
      String fromNotebookName,
      String toWorkspaceNamespace,
      String toWorkspaceFirecloudName,
      String newNotebookName) {
    workspaceAuthService.enforceWorkspaceAccessLevel(
        fromWorkspaceNamespace, fromWorkspaceFirecloudName, WorkspaceAccessLevel.READER);
    workspaceAuthService.enforceWorkspaceAccessLevel(
        toWorkspaceNamespace, toWorkspaceFirecloudName, WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateActiveBilling(toWorkspaceNamespace, toWorkspaceFirecloudName);
    newNotebookName = NotebooksService.withNotebookExtension(newNotebookName);

    final DbWorkspace fromWorkspace =
        workspaceDao.getRequired(fromWorkspaceNamespace, fromWorkspaceFirecloudName);
    final DbAccessTier fromTier = fromWorkspace.getCdrVersion().getAccessTier();
    final DbWorkspace toWorkspace =
        workspaceDao.getRequired(toWorkspaceNamespace, toWorkspaceFirecloudName);
    final DbAccessTier toTier = toWorkspace.getCdrVersion().getAccessTier();

    if (!fromTier.equals(toTier)) {
      final String msg =
          String.format(
              "Cannot copy between access tiers (attempted copy from %s to %s)",
              fromTier.getDisplayName(), toTier.getDisplayName());
      throw new BadRequestException(msg);
    }

    GoogleCloudLocators fromNotebookLocators =
        getNotebookLocators(fromWorkspaceNamespace, fromWorkspaceFirecloudName, fromNotebookName);
    GoogleCloudLocators newNotebookLocators =
        getNotebookLocators(toWorkspaceNamespace, toWorkspaceFirecloudName, newNotebookName);

    if (!cloudStorageClient
        .getExistingBlobIdsIn(Collections.singletonList(newNotebookLocators.blobId))
        .isEmpty()) {
      throw new BlobAlreadyExistsException();
    }
    cloudStorageClient.copyBlob(fromNotebookLocators.blobId, newNotebookLocators.blobId);

    FileDetail fileDetail = new FileDetail();
    fileDetail.setName(newNotebookName);
    fileDetail.setPath(newNotebookLocators.fullPath);
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    fileDetail.setLastModifiedTime(now.getTime());
    userRecentResourceService.updateNotebookEntry(
        toWorkspace.getWorkspaceId(), userProvider.get().getUserId(), newNotebookLocators.fullPath);

    return fileDetail;
  }

  @Override
  public FileDetail cloneNotebook(
      String workspaceNamespace, String workspaceName, String fromNotebookName) {
    String newName = "Duplicate of " + fromNotebookName;
    final FileDetail copiedNotebookFileDetail =
        copyNotebook(
            workspaceNamespace,
            workspaceName,
            fromNotebookName,
            workspaceNamespace,
            workspaceName,
            newName);
    logsBasedMetricService.recordEvent(EventMetric.NOTEBOOK_CLONE);
    return copiedNotebookFileDetail;
  }

  @Override
  public void deleteNotebook(String workspaceNamespace, String workspaceName, String notebookName) {
    GoogleCloudLocators notebookLocators =
        getNotebookLocators(workspaceNamespace, workspaceName, notebookName);
    cloudStorageClient.deleteBlob(notebookLocators.blobId);
    userRecentResourceService.deleteNotebookEntry(
        workspaceDao.getRequired(workspaceNamespace, workspaceName).getWorkspaceId(),
        userProvider.get().getUserId(),
        notebookLocators.fullPath);
    logsBasedMetricService.recordEvent(EventMetric.NOTEBOOK_DELETE);
  }

  @Override
  public FileDetail renameNotebook(
      String workspaceNamespace, String workspaceName, String originalName, String newName) {
    FileDetail fileDetail =
        copyNotebook(
            workspaceNamespace,
            workspaceName,
            originalName,
            workspaceNamespace,
            workspaceName,
            NotebooksService.withNotebookExtension(newName));
    deleteNotebook(workspaceNamespace, workspaceName, originalName);

    return fileDetail;
  }

  @Override
  public JSONObject getNotebookContents(String bucketName, String notebookName) {
    Blob blob = getBlobWithSizeConstraint(bucketName, notebookName);
    return cloudStorageClient.readBlobAsJson(blob);
  }

  @Override
  public KernelTypeEnum getNotebookKernel(JSONObject notebookFile) {
    try {
      String language =
          Optional.of(notebookFile.getJSONObject("metadata"))
              .flatMap(metaDataObj -> Optional.of(metaDataObj.getJSONObject("kernelspec")))
              .map(kernelSpec -> kernelSpec.getString("language"))
              .orElse("Python");

      if ("R".equals(language)) {
        return KernelTypeEnum.R;
      } else {
        return KernelTypeEnum.PYTHON;
      }
    } catch (JSONException e) {
      // If we can't find metadata to parse, default to python.
      return KernelTypeEnum.PYTHON;
    }
  }

  @Override
  public KernelTypeEnum getNotebookKernel(
      String workspaceNamespace, String workspaceName, String notebookName) {
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();

    Blob blob = getBlobWithSizeConstraint(bucketName, notebookName);
    return getNotebookKernel(cloudStorageClient.readBlobAsJson(blob));
  }

  private Blob getBlobWithSizeConstraint(String bucketName, String notebookName) {
    Blob blob =
        cloudStorageClient.getBlob(
            bucketName, "notebooks/".concat(NotebooksService.withNotebookExtension(notebookName)));
    if (blob.getSize() >= MAX_NOTEBOOK_READ_SIZE_BYTES) {
      throw new FailedPreconditionException(
          String.format(
              "target notebook is too large to process @ %.2fMB", ((double) blob.getSize()) / 1e6));
    }
    return blob;
  }

  @Override
  public void saveNotebook(String bucketName, String notebookName, JSONObject notebookContents) {
    cloudStorageClient.writeFile(
        bucketName,
        "notebooks/" + NotebooksService.withNotebookExtension(notebookName),
        notebookContents.toString().getBytes(StandardCharsets.UTF_8));
    logsBasedMetricService.recordEvent(EventMetric.NOTEBOOK_SAVE);
  }

  @Override
  public String convertNotebookToHtml(byte[] notebook) {
    // We need to send a byte array so the ApiClient attaches the body as is instead
    // of serializing it through Gson which it will do for Strings.
    // The default Gson serializer does not work since it strips out some null fields
    // which are needed for nbconvert. Skip the JSON conversion here to reduce memory overhead.
    return PREVIEW_SANITIZER.sanitize(fireCloudService.staticNotebooksConvert(notebook));
  }

  @Override
  public String getReadOnlyHtml(
      String workspaceNamespace, String workspaceName, String notebookName) {
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();

    Blob blob = getBlobWithSizeConstraint(bucketName, notebookName);
    return convertNotebookToHtml(blob.getContent());
  }

  @Override
  public String adminGetReadOnlyHtml(
      String workspaceNamespace, String workspaceName, String notebookName) {
    String bucketName =
        fireCloudService
            .getWorkspaceAsService(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();

    Blob blob = getBlobWithSizeConstraint(bucketName, notebookName);
    return convertNotebookToHtml(blob.getContent());
  }

  private GoogleCloudLocators getNotebookLocators(
      String workspaceNamespace, String firecloudName, String notebookName) {
    String bucket =
        fireCloudService
            .getWorkspace(workspaceNamespace, firecloudName)
            .getWorkspace()
            .getBucketName();
    String blobPath = NOTEBOOKS_WORKSPACE_DIRECTORY + "/" + notebookName;
    String pathStart = "gs://" + bucket + "/";
    String fullPath = pathStart + blobPath;
    BlobId blobId = BlobId.of(bucket, blobPath);
    return new GoogleCloudLocators(blobId, fullPath);
  }
}
