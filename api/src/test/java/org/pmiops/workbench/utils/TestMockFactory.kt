package org.pmiops.workbench.utils

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock

import java.util.UUID
import org.pmiops.workbench.billing.BillingProjectBufferService
import org.pmiops.workbench.db.model.BillingProjectBufferEntry
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.model.WorkspaceAccessLevel

/** Created by brubenst on 9/4/19.  */
class TestMockFactory {

    fun createFcWorkspace(
            ns: String, name: String, creator: String?): org.pmiops.workbench.firecloud.model.Workspace {
        val fcWorkspace = org.pmiops.workbench.firecloud.model.Workspace()
        fcWorkspace.setNamespace(ns)
        fcWorkspace.setWorkspaceId(ns)
        fcWorkspace.setName(name)
        fcWorkspace.setCreatedBy(creator)
        fcWorkspace.setBucketName(BUCKET_NAME)
        return fcWorkspace
    }

    fun stubCreateFcWorkspace(fireCloudService: FireCloudService) {
        doAnswer { invocation ->
            val capturedWorkspaceName = invocation.arguments[1] as String
            val capturedWorkspaceNamespace = invocation.arguments[0] as String
            val fcWorkspace = createFcWorkspace(capturedWorkspaceNamespace, capturedWorkspaceName, null)

            val fcResponse = org.pmiops.workbench.firecloud.model.WorkspaceResponse()
            fcResponse.setWorkspace(fcWorkspace)
            fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString())

            doReturn(fcResponse)
                    .`when`(fireCloudService)
                    .getWorkspace(capturedWorkspaceNamespace, capturedWorkspaceName)
            fcWorkspace
        }
                .`when`(fireCloudService)
                .createWorkspace(anyString(), anyString())
    }

    fun stubBufferBillingProject(billingProjectBufferService: BillingProjectBufferService) {
        doAnswer { invocation ->
            val entry = mock(BillingProjectBufferEntry::class.java)
            doReturn(UUID.randomUUID().toString()).`when`(entry).fireCloudProjectName
            entry
        }
                .`when`(billingProjectBufferService)
                .assignBillingProject(any<User>())
    }

    companion object {
        val BUCKET_NAME = "workspace-bucket"
    }
}
