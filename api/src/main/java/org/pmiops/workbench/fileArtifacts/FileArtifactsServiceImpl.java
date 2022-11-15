package org.pmiops.workbench.fileArtifacts;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.json.JSONException;
import org.json.JSONObject;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.BlobAlreadyExistsException;
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
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileArtifactsServiceImpl implements FileArtifactsService {

  private static final Pattern NOTEBOOK_PATTERN =
      Pattern.compile(FILE_ARTIFACTS_WORKSPACE_DIRECTORY + "/[^/]+(\\.(?i)(ipynb))$");
  private static final Pattern RMD_PATTERN =
      Pattern.compile(FILE_ARTIFACTS_WORKSPACE_DIRECTORY + "/[^/]+(\\.(?i)(rmd))$");
  // Experimentally determined that generating the preview HTML for a >11MB fileArtifact results in
  // OOMs on a default F1 240MB GAE task. OOMs may still occur during concurrent requests. If this
  // issue persists, we can move preview processing onto the client (calling Calhoun), or fully
  // client-side (using a client-side fileArtifact renderer).
  private static final long MAX_FILE_READ_SIZE_BYTES = 5 * 1000 * 1000; // 5MB
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
                  .allowElements("head", "body", "style", "pre")

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
  public FileArtifactsServiceImpl(
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
  public List<FileDetail> getFileArtifacts(String workspaceNamespace, String workspaceName) {
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();
    return getFileArtifactsAsService(bucketName, workspaceNamespace, workspaceName);
  }

  // NOTE: may be an undercount since we only retrieve the first Page of Storage List results
  @Override
  public List<FileDetail> getFileArtifactsAsService(
      String bucketName, String workspaceNamespace, String workspaceName) {
    Set<String> workspaceUsers =
        workspaceAuthService.getFirecloudWorkspaceAcls(workspaceNamespace, workspaceName).keySet();
    return cloudStorageClient.getBlobPageForPrefix(bucketName, FILE_ARTIFACTS_WORKSPACE_DIRECTORY)
        .stream()
        .filter(this::isFileArtifactBlob)
        .map(blob -> cloudStorageClient.blobToFileDetail(blob, bucketName, workspaceUsers))
        .collect(Collectors.toList());
  }

  @Override
  public boolean isFileArtifactBlob(Blob blob) {
    return NOTEBOOK_PATTERN.matcher(blob.getName()).matches();
  }

  @Override
  public FileDetail copyFileArtifact(
      String fromWorkspaceNamespace,
      String fromWorkspaceFirecloudName,
      String fromFileArtifactName,
      String toWorkspaceNamespace,
      String toWorkspaceFirecloudName,
      String newFileArtifactName) {
    workspaceAuthService.enforceWorkspaceAccessLevel(
        fromWorkspaceNamespace, fromWorkspaceFirecloudName, WorkspaceAccessLevel.READER);
    workspaceAuthService.enforceWorkspaceAccessLevel(
        toWorkspaceNamespace, toWorkspaceFirecloudName, WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateActiveBilling(toWorkspaceNamespace, toWorkspaceFirecloudName);
    newFileArtifactName = FileArtifactsService.withFileArtifactExtension(newFileArtifactName);

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

    GoogleCloudLocators fromFileArtifactLocators =
        getFileArtifactLocators(fromWorkspaceNamespace, fromWorkspaceFirecloudName, fromFileArtifactName);
    GoogleCloudLocators newFileArtifactLocators =
        getFileArtifactLocators(toWorkspaceNamespace, toWorkspaceFirecloudName, newFileArtifactName);

    if (!cloudStorageClient
        .getExistingBlobIdsIn(Collections.singletonList(newFileArtifactLocators.blobId))
        .isEmpty()) {
      throw new BlobAlreadyExistsException();
    }
    cloudStorageClient.copyBlob(fromFileArtifactLocators.blobId, newFileArtifactLocators.blobId);

    FileDetail fileDetail = new FileDetail();
    fileDetail.setName(newFileArtifactName);
    fileDetail.setPath(newFileArtifactLocators.fullPath);
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    fileDetail.setLastModifiedTime(now.getTime());
    userRecentResourceService.updateFileArtifactEntry(
        toWorkspace.getWorkspaceId(), userProvider.get().getUserId(), newFileArtifactLocators.fullPath);

    return fileDetail;
  }

  @Override
  public FileDetail cloneFileArtifact(
      String workspaceNamespace, String workspaceName, String fromFileArtifactName) {
    String newName = "Duplicate of " + fromFileArtifactName;
    final FileDetail copiedFileArtifactFileDetail =
        copyFileArtifact(
            workspaceNamespace,
            workspaceName,
            fromFileArtifactName,
            workspaceNamespace,
            workspaceName,
            newName);
    logsBasedMetricService.recordEvent(EventMetric.NOTEBOOK_CLONE);
    return copiedFileArtifactFileDetail;
  }

  @Override
  public void deleteFileArtifact(String workspaceNamespace, String workspaceName, String fileArtifactName) {
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceName, WorkspaceAccessLevel.WRITER);

    GoogleCloudLocators fileArtifactLocators =
        getFileArtifactLocators(workspaceNamespace, workspaceName, fileArtifactName);
    cloudStorageClient.deleteBlob(fileArtifactLocators.blobId);
    userRecentResourceService.deleteFileArtifactEntry(
        workspaceDao.getRequired(workspaceNamespace, workspaceName).getWorkspaceId(),
        userProvider.get().getUserId(),
        fileArtifactLocators.fullPath);
    logsBasedMetricService.recordEvent(EventMetric.NOTEBOOK_DELETE);
  }

  @Override
  public FileDetail renameFileArtifact(
      String workspaceNamespace, String workspaceName, String originalName, String newName) {
    FileDetail fileDetail =
        copyFileArtifact(
            workspaceNamespace,
            workspaceName,
            originalName,
            workspaceNamespace,
            workspaceName,
            FileArtifactsService.withFileArtifactExtension(newName));
    deleteFileArtifact(workspaceNamespace, workspaceName, originalName);

    return fileDetail;
  }

  @Override
  public JSONObject getFileArtifactContents(String bucketName, String fileArtifactName) {
    Blob blob = getBlobWithSizeConstraint(bucketName, fileArtifactName);
    return cloudStorageClient.readBlobAsJson(blob);
  }

  @Override
  public KernelTypeEnum getFileArtifactKernel(JSONObject fileArtifactFile) {
    try {
      String language =
          Optional.of(fileArtifactFile.getJSONObject("metadata"))
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
  public KernelTypeEnum getFileArtifactKernel(
      String workspaceNamespace, String workspaceName, String fileArtifactName) {
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();

    Blob blob = getBlobWithSizeConstraint(bucketName, fileArtifactName);
    return getFileArtifactKernel(cloudStorageClient.readBlobAsJson(blob));
  }

  private Blob getBlobWithSizeConstraint(String bucketName, String fileArtifactName) {
    Blob blob =
        cloudStorageClient.getBlob(
            bucketName, "fileArtifacts/".concat(FileArtifactsService.withFileArtifactExtension(fileArtifactName)));
    if (blob.getSize() >= MAX_FILE_READ_SIZE_BYTES) {
      throw new FailedPreconditionException(
          String.format(
              "target fileArtifact is too large to process @ %.2fMB", ((double) blob.getSize()) / 1e6));
    }
    return blob;
  }

  @Override
  public void saveFileArtifact(String bucketName, String fileArtifactName, JSONObject fileArtifactContents) {
    cloudStorageClient.writeFile(
        bucketName,
        "fileArtifacts/" + FileArtifactsService.withFileArtifactExtension(fileArtifactName),
        fileArtifactContents.toString().getBytes(StandardCharsets.UTF_8));
    logsBasedMetricService.recordEvent(EventMetric.NOTEBOOK_SAVE);
  }

  @Override
  public String convertFileArtifactToHtml(byte[] fileArtifact) {
    // We need to send a byte array so the ApiClient attaches the body as is instead
    // of serializing it through Gson which it will do for Strings.
    // The default Gson serializer does not work since it strips out some null fields
    // which are needed for nbconvert. Skip the JSON conversion here to reduce memory overhead.
    return PREVIEW_SANITIZER.sanitize(fireCloudService.staticFileArtifactsConvert(fileArtifact));
  }

  @Override
  public String getReadOnlyHtml(
      String workspaceNamespace, String workspaceName, String fileArtifactName) {
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();

    Blob blob = getBlobWithSizeConstraint(bucketName, fileArtifactName);
    return convertFileArtifactToHtml(blob.getContent());
  }

  @Override
  public String adminGetReadOnlyHtml(
      String workspaceNamespace, String workspaceName, String fileArtifactName) {
    String bucketName =
        fireCloudService
            .getWorkspaceAsService(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();

    Blob blob = getBlobWithSizeConstraint(bucketName, fileArtifactName);
    return convertFileArtifactToHtml(blob.getContent());
  }

  private GoogleCloudLocators getFileArtifactLocators(
      String workspaceNamespace, String firecloudName, String fileArtifactName) {
    String bucket =
        fireCloudService
            .getWorkspace(workspaceNamespace, firecloudName)
            .getWorkspace()
            .getBucketName();
    String blobPath = FILE_ARTIFACTS_WORKSPACE_DIRECTORY + "/" + fileArtifactName;
    String pathStart = "gs://" + bucket + "/";
    String fullPath = pathStart + blobPath;
    BlobId blobId = BlobId.of(bucket, blobPath);
    return new GoogleCloudLocators(blobId, fullPath);
  }
}
