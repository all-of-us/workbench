package org.pmiops.workbench.notebooks

import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import java.nio.charset.StandardCharsets
import java.sql.Timestamp
import java.time.Clock
import java.util.Collections
import java.util.regex.Pattern
import java.util.stream.Collectors
import javax.inject.Provider
import org.json.JSONObject
import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory
import org.owasp.html.Sanitizers
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.google.GoogleCloudLocators
import org.pmiops.workbench.model.FileDetail
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NotebooksServiceImpl @Autowired
constructor(
        private val clock: Clock,
        private val cloudStorageService: CloudStorageService,
        private val fireCloudService: FireCloudService,
        private val userProvider: Provider<User>,
        private val userRecentResourceService: UserRecentResourceService,
        private val workspaceService: WorkspaceService) : NotebooksService {

    override fun getNotebooks(workspaceNamespace: String, workspaceName: String): List<FileDetail> {
        val bucketName = fireCloudService
                .getWorkspace(workspaceNamespace, workspaceName)
                .getWorkspace()
                .getBucketName()

        return cloudStorageService.getBlobListForPrefix(bucketName, NotebooksService.NOTEBOOKS_WORKSPACE_DIRECTORY)
                .stream()
                .filter { blob -> NotebooksService.NOTEBOOK_PATTERN.matcher(blob.name).matches() }
                .map<Any> { blob -> blobToFileDetail(blob, bucketName) }
                .collect<List<FileDetail>, Any>(Collectors.toList())
    }

    private fun blobToFileDetail(blob: Blob, bucketName: String): FileDetail {
        val parts = blob.name.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val fileDetail = FileDetail()
        fileDetail.setName(parts[parts.size - 1])
        fileDetail.setPath("gs://" + bucketName + "/" + blob.name)
        fileDetail.setLastModifiedTime(blob.updateTime)
        return fileDetail
    }

    override fun copyNotebook(
            fromWorkspaceNamespace: String,
            fromWorkspaceName: String,
            fromNotebookName: String,
            toWorkspaceNamespace: String,
            toWorkspaceName: String,
            newNotebookName: String): FileDetail {
        var newNotebookName = newNotebookName
        newNotebookName = NotebooksService.withNotebookExtension(newNotebookName)
        val fromNotebookLocators = getNotebookLocators(fromWorkspaceNamespace, fromWorkspaceName, fromNotebookName)
        val newNotebookLocators = getNotebookLocators(toWorkspaceNamespace, toWorkspaceName, newNotebookName)

        workspaceService.enforceWorkspaceAccessLevel(
                fromWorkspaceNamespace, fromWorkspaceName, WorkspaceAccessLevel.READER)
        workspaceService.enforceWorkspaceAccessLevel(
                toWorkspaceNamespace, toWorkspaceName, WorkspaceAccessLevel.WRITER)
        if (!cloudStorageService
                        .blobsExist(listOf(newNotebookLocators.blobId))
                        .isEmpty()) {
            throw BlobAlreadyExistsException()
        }
        cloudStorageService.copyBlob(fromNotebookLocators.blobId, newNotebookLocators.blobId)

        val fileDetail = FileDetail()
        fileDetail.setName(newNotebookName)
        fileDetail.setPath(newNotebookLocators.fullPath)
        val now = Timestamp(clock.instant().toEpochMilli())
        fileDetail.setLastModifiedTime(now.time)
        userRecentResourceService.updateNotebookEntry(
                workspaceService.getRequired(toWorkspaceNamespace, toWorkspaceName).workspaceId,
                userProvider.get().userId,
                newNotebookLocators.fullPath,
                now)

        return fileDetail
    }

    override fun cloneNotebook(
            workspaceNamespace: String, workspaceName: String, fromNotebookName: String): FileDetail {
        val newName = "Duplicate of $fromNotebookName"
        return copyNotebook(
                workspaceNamespace,
                workspaceName,
                fromNotebookName,
                workspaceNamespace,
                workspaceName,
                newName)
    }

    override fun deleteNotebook(workspaceNamespace: String, workspaceName: String, notebookName: String) {
        val notebookLocators = getNotebookLocators(workspaceNamespace, workspaceName, notebookName)
        cloudStorageService.deleteBlob(notebookLocators.blobId)
        userRecentResourceService.deleteNotebookEntry(
                workspaceService.getRequired(workspaceNamespace, workspaceName).workspaceId,
                userProvider.get().userId,
                notebookLocators.fullPath)
    }

    override fun renameNotebook(
            workspaceNamespace: String, workspaceName: String, originalName: String, newName: String): FileDetail {
        val fileDetail = copyNotebook(
                workspaceNamespace,
                workspaceName,
                originalName,
                workspaceNamespace,
                workspaceName,
                NotebooksService.withNotebookExtension(newName))
        deleteNotebook(workspaceNamespace, workspaceName, originalName)

        return fileDetail
    }

    override fun getNotebookContents(bucketName: String, notebookName: String): JSONObject {
        return cloudStorageService.getFileAsJson(
                bucketName, "notebooks/" + NotebooksService.withNotebookExtension(notebookName))
    }

    override fun saveNotebook(bucketName: String, notebookName: String, notebookContents: JSONObject) {
        cloudStorageService.writeFile(
                bucketName,
                "notebooks/" + NotebooksService.withNotebookExtension(notebookName),
                notebookContents.toString().toByteArray(StandardCharsets.UTF_8))
    }

    override fun getReadOnlyHtml(
            workspaceNamespace: String, workspaceName: String, notebookName: String): String {
        val bucketName = fireCloudService
                .getWorkspace(workspaceNamespace, workspaceName)
                .getWorkspace()
                .getBucketName()

        // We need to send a byte array so the ApiClient attaches the body as is instead
        // of serializing it through Gson which it will do for Strings.
        // The default Gson serializer does not work since it strips out some null fields
        // which are needed for nbconvert
        val contents = getNotebookContents(bucketName, notebookName).toString().toByteArray()
        return PREVIEW_SANITIZER.sanitize(fireCloudService.staticNotebooksConvert(contents))
    }

    private fun getNotebookLocators(
            workspaceNamespace: String, workspaceName: String, notebookName: String): GoogleCloudLocators {
        val bucket = fireCloudService
                .getWorkspace(workspaceNamespace, workspaceName)
                .getWorkspace()
                .getBucketName()
        val blobPath = NotebooksService.NOTEBOOKS_WORKSPACE_DIRECTORY + "/" + notebookName
        val pathStart = "gs://$bucket/"
        val fullPath = pathStart + blobPath
        val blobId = BlobId.of(bucket, blobPath)
        return GoogleCloudLocators(blobId, fullPath)
    }

    companion object {

        private val PREVIEW_SANITIZER = Sanitizers.FORMATTING
                .and(Sanitizers.BLOCKS)
                .and(Sanitizers.LINKS)
                .and(Sanitizers.STYLES)
                .and(Sanitizers.TABLES)
                .and(
                        HtmlPolicyBuilder()
                                .allowElements("img")
                                .allowUrlProtocols("data")
                                .allowAttributes("src")
                                .matching(Pattern.compile("^data:image.*"))
                                .onElements("img")
                                .toFactory())
                .and(
                        HtmlPolicyBuilder()
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
                                .toFactory())
    }
}
