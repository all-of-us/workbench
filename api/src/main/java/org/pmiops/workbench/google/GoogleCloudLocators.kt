package org.pmiops.workbench.google

import com.google.cloud.storage.BlobId
import java.util.Objects

class GoogleCloudLocators(val blobId: BlobId, val fullPath: String) {

    override fun hashCode(): Int {
        return Objects.hash(blobId, fullPath)
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is GoogleCloudLocators) {
            return false
        }
        val that = obj as GoogleCloudLocators?
        return this.blobId == that!!.blobId && this.fullPath == that.fullPath
    }
}
