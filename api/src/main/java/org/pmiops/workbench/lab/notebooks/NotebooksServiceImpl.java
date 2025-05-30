package org.pmiops.workbench.lab.notebooks;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import jakarta.inject.Provider;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import org.pmiops.workbench.exceptions.NotImplementedException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.GoogleCloudLocators;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotebooksServiceImpl implements NotebooksService {
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

  @Autowired
  public NotebooksServiceImpl(
      Clock clock,
      CloudStorageClient cloudStorageClient,
      FireCloudService fireCloudService,
      Provider<DbUser> userProvider,
      UserRecentResourceService userRecentResourceService,
      WorkspaceDao workspaceDao,
      WorkspaceAuthService workspaceAuthService) {
    this.clock = clock;
    this.cloudStorageClient = cloudStorageClient;
    this.fireCloudService = fireCloudService;
    this.userProvider = userProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.workspaceDao = workspaceDao;
    this.workspaceAuthService = workspaceAuthService;
  }

  // NOTE: may be an undercount since we only retrieve the first Page of Storage List results
  @Override
  public List<FileDetail> getNotebooks(String workspaceNamespace, String workspaceTerraName) {
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceTerraName)
            .getWorkspace()
            .getBucketName();
    return getNotebooksAsService(bucketName, workspaceNamespace, workspaceTerraName);
  }

  // NOTE: may be an undercount since we only retrieve the first Page of Storage List results
  @Override
  public List<FileDetail> getNotebooksAsService(
      String bucketName, String workspaceNamespace, String workspaceTerraName) {
    Set<String> workspaceUsers =
        workspaceAuthService
            .getFirecloudWorkspaceAcl(workspaceNamespace, workspaceTerraName)
            .keySet();
    return cloudStorageClient
        .getBlobPageForPrefix(bucketName, NotebookUtils.NOTEBOOKS_WORKSPACE_DIRECTORY)
        .stream()
        .filter(this::isManagedNotebookBlob)
        .map(blob -> cloudStorageClient.blobToFileDetail(blob, bucketName, workspaceUsers))
        .collect(Collectors.toList());
  }

  @Override
  public List<FileDetail> getAllNotebooksByAppType(
      String bucketName, String workspaceNamespace, String workspaceTerraName, AppType appType) {
    List<FileDetail> allNotebooks =
        getNotebooksAsService(bucketName, workspaceNamespace, workspaceTerraName);
    if (appType.equals(AppType.RSTUDIO)) {
      return allNotebooks.stream()
          .filter(fileDetail -> NotebookUtils.isRStudioFile(fileDetail.getName()))
          .collect(Collectors.toList());
    } else if (appType.equals(AppType.SAS)) {
      return allNotebooks.stream()
          .filter(fileDetail -> NotebookUtils.isSasFile(fileDetail.getName()))
          .collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }
  }

  @Override
  public List<FileDetail> getAllJupyterNotebooks(
      String bucketName, String workspaceNamespace, String workspaceTerraName) {
    return getNotebooksAsService(bucketName, workspaceNamespace, workspaceTerraName).stream()
        .filter(fileDetail -> NotebookUtils.isJupyterNotebook(fileDetail.getName()))
        .collect(Collectors.toList());
  }

  @Override
  public boolean isManagedNotebookBlob(Blob blob) {
    // Blobs have notebooks/ directory
    return NotebookUtils.isJupyterNotebookWithDirectory(blob.getName())
        || NotebookUtils.isRStudioFileWithDirectory(blob.getName())
        || NotebookUtils.isSasWithDirectory(blob.getName());
  }

  @Override
  public FileDetail copyNotebook(
      String fromWorkspaceNamespace,
      String fromWorkspaceFirecloudName,
      String fromNotebookNameWithExtension,
      String toWorkspaceNamespace,
      String toWorkspaceFirecloudName,
      String newNotebookNameWithExtension) {
    workspaceAuthService.enforceWorkspaceAccessLevel(
        fromWorkspaceNamespace, fromWorkspaceFirecloudName, WorkspaceAccessLevel.READER);
    workspaceAuthService.enforceWorkspaceAccessLevel(
        toWorkspaceNamespace, toWorkspaceFirecloudName, WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateInitialCreditUsage(toWorkspaceNamespace, toWorkspaceFirecloudName);

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
        getNotebookLocators(
            fromWorkspaceNamespace, fromWorkspaceFirecloudName, fromNotebookNameWithExtension);
    GoogleCloudLocators newNotebookLocators =
        getNotebookLocators(
            toWorkspaceNamespace, toWorkspaceFirecloudName, newNotebookNameWithExtension);

    if (!cloudStorageClient
        .getExistingBlobIdsIn(Collections.singletonList(newNotebookLocators.blobId))
        .isEmpty()) {
      throw new BlobAlreadyExistsException();
    }
    cloudStorageClient.copyBlob(fromNotebookLocators.blobId, newNotebookLocators.blobId);

    FileDetail fileDetail = new FileDetail();
    fileDetail.setName(newNotebookNameWithExtension);
    fileDetail.setPath(newNotebookLocators.fullPath);
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    fileDetail.setLastModifiedTime(now.getTime());
    userRecentResourceService.updateNotebookEntry(
        toWorkspace.getWorkspaceId(), userProvider.get().getUserId(), newNotebookLocators.fullPath);

    return fileDetail;
  }

  @Override
  public FileDetail cloneNotebook(
      String workspaceNamespace, String workspaceTerraName, String notebookNameWithExtension) {
    String newNameWithExtension = "Duplicate of " + notebookNameWithExtension;
    return copyNotebook(
        workspaceNamespace,
        workspaceTerraName,
        notebookNameWithExtension,
        workspaceNamespace,
        workspaceTerraName,
        newNameWithExtension);
  }

  @Override
  public void deleteNotebook(
      String workspaceNamespace, String workspaceTerraName, String notebookName) {
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceTerraName, WorkspaceAccessLevel.WRITER);

    GoogleCloudLocators notebookLocators =
        getNotebookLocators(workspaceNamespace, workspaceTerraName, notebookName);
    cloudStorageClient.deleteBlob(notebookLocators.blobId);
    userRecentResourceService.deleteNotebookEntry(
        workspaceDao.getRequired(workspaceNamespace, workspaceTerraName).getWorkspaceId(),
        userProvider.get().getUserId(),
        notebookLocators.fullPath);
  }

  @Override
  public FileDetail renameNotebook(
      String workspaceNamespace,
      String workspaceTerraName,
      String originalNameWithExtension,
      String newNameWithExtension) {
    FileDetail fileDetail =
        copyNotebook(
            workspaceNamespace,
            workspaceTerraName,
            originalNameWithExtension,
            workspaceNamespace,
            workspaceTerraName,
            newNameWithExtension);
    deleteNotebook(workspaceNamespace, workspaceTerraName, originalNameWithExtension);

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
              .map(metaDataObj -> metaDataObj.getJSONObject("kernelspec"))
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
      String workspaceNamespace, String workspaceTerraName, String notebookName) {
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceTerraName)
            .getWorkspace()
            .getBucketName();

    return getNotebookKernel(getNotebookContents(bucketName, notebookName));
  }

  private Blob getBlobWithSizeConstraint(String bucketName, String notebookName) {
    Blob blob =
        cloudStorageClient.getBlob(bucketName, NotebookUtils.withNotebookPath(notebookName));
    if (blob.getSize() >= MAX_NOTEBOOK_READ_SIZE_BYTES) {
      throw new FailedPreconditionException(
          String.format(
              "target notebook is too large to process @ %.2fMB", ((double) blob.getSize()) / 1e6));
    }
    return blob;
  }

  @Override
  public void saveNotebook(
      String bucketName, String notebookNameWithFileExtension, JSONObject notebookContents) {
    if (!NotebookUtils.isJupyterNotebook(notebookNameWithFileExtension)) {
      throw new NotImplementedException(
          String.format(
              "%s is a type of file that is not yet supported", notebookNameWithFileExtension));
    }

    cloudStorageClient.writeFile(
        bucketName,
        NotebookUtils.withNotebookPath(
            NotebookUtils.withJupyterNotebookExtension(notebookNameWithFileExtension)),
        notebookContents.toString().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String convertJupyterNotebookToHtml(byte[] notebook) {
    // We need to send a byte array so the ApiClient attaches the body as is instead
    // of serializing it through Gson which it will do for Strings.
    // The default Gson serializer does not work since it strips out some null fields
    // which are needed for nbconvert. Skip the JSON conversion here to reduce memory overhead.
    return PREVIEW_SANITIZER.sanitize(fireCloudService.staticJupyterNotebooksConvert(notebook));
  }

  @Override
  public String getReadOnlyHtml(
      String workspaceNamespace, String workspaceTerraName, String notebookName) {
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceTerraName)
            .getWorkspace()
            .getBucketName();

    Blob blob = getBlobWithSizeConstraint(bucketName, notebookName);
    return getReadOnlyHtml(blob, notebookName);
  }

  @Override
  public String adminGetReadOnlyHtml(
      String workspaceNamespace, String workspaceTerraName, String notebookNameWithFileExtension) {
    String bucketName =
        fireCloudService
            .getWorkspaceAsService(workspaceNamespace, workspaceTerraName)
            .getWorkspace()
            .getBucketName();

    Blob blob = getBlobWithSizeConstraint(bucketName, notebookNameWithFileExtension);
    return getReadOnlyHtml(blob, notebookNameWithFileExtension);
  }

  private String getReadOnlyHtml(Blob blob, String notebookNameWithFileExtension) {
    if (NotebookUtils.isJupyterNotebook(notebookNameWithFileExtension)) {
      return convertJupyterNotebookToHtml(blob.getContent());
    } else if (NotebookUtils.isRStudioFile(notebookNameWithFileExtension)) {
      return convertRstudioNotebookToHtml(blob.getContent());
    } else if (NotebookUtils.isSasFile(notebookNameWithFileExtension)) {
      return convertSasNotebookToHtml(blob.getContent());
    } else {
      throw new NotImplementedException(
          String.format(
              "Converting %s to read-only HTML is not supported", notebookNameWithFileExtension));
    }
  }

  private GoogleCloudLocators getNotebookLocators(
      String workspaceNamespace, String firecloudName, String notebookName) {
    String bucket =
        fireCloudService
            .getWorkspace(workspaceNamespace, firecloudName)
            .getWorkspace()
            .getBucketName();
    String blobPath = NotebookUtils.withNotebookPath(notebookName);
    String pathStart = "gs://" + bucket + "/";
    String fullPath = pathStart + blobPath;
    BlobId blobId = BlobId.of(bucket, blobPath);
    return new GoogleCloudLocators(blobId, fullPath);
  }

  private String convertRstudioNotebookToHtml(byte[] notebook) {
    // We need to send a byte array so the ApiClient attaches the body as is instead
    // of serializing it through Gson which it will do for Strings.
    // The default Gson serializer does not work since it strips out some null fields
    // which are needed for nbconvert. Skip the JSON conversion here to reduce memory overhead.
    return PREVIEW_SANITIZER.sanitize(fireCloudService.staticRstudioNotebooksConvert(notebook));
  }

  private String convertSasNotebookToHtml(byte[] notebook) {
    // preserve newline
    return new String(notebook, StandardCharsets.UTF_8).replace("\n", "<br/>");
  }
}
