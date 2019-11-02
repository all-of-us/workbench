package org.pmiops.workbench.api

import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.model.StatusResponse
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class StatusController @Autowired
internal constructor(
        private val fireCloudService: FireCloudService, private val leonardoNotebooksClient: LeonardoNotebooksClient) : StatusApiDelegate {

    val status: ResponseEntity<StatusResponse>
        get() {
            val statusResponse = StatusResponse()
            statusResponse.setFirecloudStatus(fireCloudService.firecloudStatus)
            statusResponse.setNotebooksStatus(leonardoNotebooksClient.notebooksStatus)
            return ResponseEntity.ok<StatusResponse>(statusResponse)
        }
}
