package org.pmiops.workbench.notebooks

import org.pmiops.workbench.exceptions.WorkbenchException
import org.pmiops.workbench.notebooks.model.Cluster
import org.pmiops.workbench.notebooks.model.StorageLink

/**
 * Encapsulate Leonardo's Notebooks API interaction details and provide a simple/mockable interface
 * for internal use.
 */
interface LeonardoNotebooksClient {

    /** @return true if notebooks is okay, false if notebooks are down.
     */
    val notebooksStatus: Boolean

    /**
     * Creates a notebooks cluster owned by the current authenticated user.
     *
     * @param googleProject the google project that will be used for this notebooks cluster
     * @param clusterName the user assigned/auto-generated name for this notebooks cluster
     * @param cdrVersion the version of the CDR that the workspace to which this notebook belongs is
     * using
     */
    @Throws(WorkbenchException::class)
    fun createCluster(googleProject: String, clusterName: String, cdrVersion: String): Cluster

    /** Deletes a notebook cluster  */
    @Throws(WorkbenchException::class)
    fun deleteCluster(googleProject: String, clusterName: String)

    /** Lists all existing clusters  */
    @Throws(WorkbenchException::class)
    fun listClusters(labels: String, includeDeleted: Boolean): List<Cluster>

    /** Gets information about a notebook cluster  */
    @Throws(WorkbenchException::class)
    fun getCluster(googleProject: String, clusterName: String): Cluster

    /** Send files over to notebook Cluster  */
    @Throws(WorkbenchException::class)
    fun localize(googleProject: String, clusterName: String, fileList: Map<String, String>)

    /** Create a new data synchronization storage link on a Welder-enabled cluster.  */
    fun createStorageLink(googleProject: String, clusterName: String, storageLink: StorageLink): StorageLink
}
