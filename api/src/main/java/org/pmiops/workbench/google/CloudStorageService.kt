package org.pmiops.workbench.google

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import java.io.IOException
import org.json.JSONObject

/** Encapsulate Google APIs for interfacing with Google Cloud Storage.  */
interface CloudStorageService {

    val jiraCredentials: JSONObject

    val elasticCredentials: JSONObject

    val gSuiteAdminCredentials: GoogleCredential

    val fireCloudAdminCredentials: GoogleCredential

    val cloudResourceManagerAdminCredentials: GoogleCredential

    val defaultServiceAccountCredentials: GoogleCredential

    val moodleApiKey: String

    fun readInvitationKey(): String

    fun readMandrillApiKey(): String

    fun getImageUrl(image_name: String): String

    fun getBlobList(bucketName: String): List<Blob>

    fun getBlobListForPrefix(bucketName: String, directory: String): List<Blob>

    fun blobsExist(id: List<BlobId>): Set<BlobId>

    fun writeFile(bucketName: String, fileName: String, bytes: ByteArray)

    fun copyBlob(from: BlobId, to: BlobId)

    @Throws(IOException::class)
    fun getGarbageCollectionServiceAccountCredentials(garbageCollectionEmail: String): GoogleCredential

    fun getFileAsJson(bucketName: String, fileName: String): JSONObject

    fun getMetadata(bucketName: String, objectPath: String): Map<String, String>

    fun deleteBlob(blobId: BlobId)
}
