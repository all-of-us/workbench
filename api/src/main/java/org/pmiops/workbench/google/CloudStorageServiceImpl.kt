package org.pmiops.workbench.google

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.CopyWriter
import com.google.cloud.storage.Storage
import com.google.cloud.storage.Storage.CopyRequest
import com.google.cloud.storage.StorageOptions
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Objects
import java.util.stream.Collectors
import javax.inject.Provider
import org.json.JSONObject
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.exceptions.NotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.String.Companion

@Service
class CloudStorageServiceImpl @Autowired
constructor(internal val configProvider: Provider<WorkbenchConfig>) : CloudStorageService {

    override val moodleApiKey: String
        get() = readCredentialsBucketString("moodle-key.txt")

    private val credentialsBucketName: String
        get() = configProvider.get().googleCloudStorageService.credentialsBucketName

    internal val imagesBucketName: String
        get() = configProvider.get().googleCloudStorageService.emailImagesBucketName

    override val jiraCredentials: JSONObject
        get() = getCredentialsBucketJSON("jira-login.json")

    override val elasticCredentials: JSONObject
        get() = getCredentialsBucketJSON("elastic-cloud.json")

    override val gSuiteAdminCredentials: GoogleCredential
        @Throws(IOException::class)
        get() = getCredential("gsuite-admin-sa.json")

    override val fireCloudAdminCredentials: GoogleCredential
        @Throws(IOException::class)
        get() = getCredential("firecloud-admin-sa.json")

    override val cloudResourceManagerAdminCredentials: GoogleCredential
        @Throws(IOException::class)
        get() = getCredential("cloud-resource-manager-admin-sa.json")

    override val defaultServiceAccountCredentials: GoogleCredential
        @Throws(IOException::class)
        get() = getCredential("app-engine-default-sa.json")

    override fun readInvitationKey(): String {
        return readCredentialsBucketString("invitation-key.txt")
    }

    override fun readMandrillApiKey(): String {
        val mandrillKeys = getCredentialsBucketJSON("mandrill-keys.json")
        return mandrillKeys.getString("api-key")
    }

    override fun getImageUrl(image_name: String): String {
        return "http://storage.googleapis.com/$imagesBucketName/$image_name"
    }

    override fun getBlobList(bucketName: String): List<Blob> {
        val storage = StorageOptions.getDefaultInstance().service
        val blobList = storage.get(bucketName).list().values
        return ImmutableList.copyOf(blobList)
    }

    override fun getBlobListForPrefix(bucketName: String, directory: String): List<Blob> {
        val storage = StorageOptions.getDefaultInstance().service
        val blobList = storage.get(bucketName).list(Storage.BlobListOption.prefix(directory)).values
        return ImmutableList.copyOf(blobList)
    }

    private fun readToString(bucketName: String, objectPath: String): String {
        return String(getBlob(bucketName, objectPath).getContent()).trim { it <= ' ' }
    }

    // wrapper for storage.get() which throws NotFoundException instead of NullPointerException
    private fun getBlob(bucketName: String, objectPath: String): Blob {
        val storage = StorageOptions.getDefaultInstance().service
        return storage.get(bucketName, objectPath)
                ?: throw NotFoundException(String.format("Bucket %s, Object %s", bucketName, objectPath))
    }

    override fun copyBlob(from: BlobId, to: BlobId) {
        val storage = StorageOptions.getDefaultInstance().service
        // Clears user-defined metadata, e.g. locking information on notebooks.
        val toInfo = BlobInfo.newBuilder(to).build()
        val w = storage.copy(CopyRequest.newBuilder().setSource(from).setTarget(toInfo).build())
        while (!w.isDone) {
            w.copyChunk()
        }
    }

    override fun writeFile(bucketName: String, fileName: String, bytes: ByteArray) {
        val storage = StorageOptions.getDefaultInstance().service
        val blobId = BlobId.of(bucketName, fileName)
        val blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build()
        storage.create(blobInfo, bytes)
    }

    private fun getCredentialsBucketJSON(objectPath: String): JSONObject {
        return JSONObject(readCredentialsBucketString(objectPath))
    }

    private fun readCredentialsBucketString(objectPath: String): String {
        return readToString(credentialsBucketName, objectPath)
    }

    @Throws(IOException::class)
    private fun getCredential(objectPath: String): GoogleCredential {
        val json = readCredentialsBucketString(objectPath)
        return GoogleCredential.fromStream(ByteArrayInputStream(json.toByteArray()))
    }

    @Throws(IOException::class)
    override fun getGarbageCollectionServiceAccountCredentials(
            garbageCollectionEmail: String): GoogleCredential {
        val objectPath = String.format("garbage-collection/%s.json", garbageCollectionEmail)
        return getCredential(objectPath)
    }

    override fun getFileAsJson(bucketName: String, fileName: String): JSONObject {
        return JSONObject(readToString(bucketName, fileName))
    }

    override fun getMetadata(bucketName: String, objectPath: String): Map<String, String> {
        return getBlob(bucketName, objectPath).metadata
    }

    override fun deleteBlob(blobId: BlobId) {
        val storage = StorageOptions.getDefaultInstance().service
        storage.delete(blobId)
    }

    override fun blobsExist(ids: List<BlobId>): Set<BlobId> {
        return if (ids.isEmpty()) {
            ImmutableSet.of()
        } else StorageOptions.getDefaultInstance().service.get(ids).stream()
                .filter(Predicate<Blob> { Objects.nonNull(it) })
                // Clear the "generation" of the blob ID for better symmetry to the input.
                .map { b -> BlobId.of(b.bucket, b.name) }
                .collect<Set<BlobId>, Any>(Collectors.toSet())
    }
}
