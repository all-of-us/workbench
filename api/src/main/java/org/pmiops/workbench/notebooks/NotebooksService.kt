package org.pmiops.workbench.notebooks

import java.util.regex.Pattern
import org.json.JSONObject
import org.pmiops.workbench.model.FileDetail

interface NotebooksService {

    fun getNotebooks(workspaceNamespace: String, workspaceName: String): List<FileDetail>

    fun copyNotebook(
            fromWorkspaceNamespace: String,
            fromWorkspaceName: String,
            fromNotebookName: String,
            toWorkspaceNamespace: String,
            toWorkspaceName: String,
            newNotebookName: String): FileDetail

    fun cloneNotebook(workspaceNamespace: String, workspaceName: String, notebookName: String): FileDetail

    fun deleteNotebook(workspaceNamespace: String, workspaceName: String, notebookName: String)

    fun renameNotebook(
            workspaceNamespace: String, workspaceName: String, notebookName: String, newName: String): FileDetail

    fun getNotebookContents(bucketName: String, notebookName: String): JSONObject

    fun saveNotebook(bucketName: String, notebookName: String, notebookContents: JSONObject)

    fun getReadOnlyHtml(workspaceNamespace: String, workspaceName: String, notebookName: String): String

    companion object {

        val NOTEBOOKS_WORKSPACE_DIRECTORY = "notebooks"
        val NOTEBOOK_EXTENSION = ".ipynb"
        val NOTEBOOK_PATTERN = Pattern.compile("$NOTEBOOKS_WORKSPACE_DIRECTORY/[^/]+(\\.(?i)(ipynb))$")

        fun withNotebookExtension(notebookName: String): String {
            return if (notebookName.endsWith(NOTEBOOK_EXTENSION))
                notebookName
            else
                notebookName + NOTEBOOK_EXTENSION
        }
    }
}
