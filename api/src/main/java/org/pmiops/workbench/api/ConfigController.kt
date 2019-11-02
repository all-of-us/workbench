package org.pmiops.workbench.api

import javax.inject.Provider
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.model.ConfigResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ConfigController @Autowired
internal constructor(private val configProvider: Provider<WorkbenchConfig>) : ConfigApiDelegate {

    val config: ResponseEntity<ConfigResponse>
        get() {
            val config = configProvider.get()
            return ResponseEntity.ok(
                    ConfigResponse()
                            .gsuiteDomain(config.googleDirectoryService.gSuiteDomain)
                            .projectId(config.server.projectId)
                            .publicApiKeyForErrorReports(config.server.publicApiKeyForErrorReports)
                            .enableComplianceTraining(config.access.enableComplianceTraining)
                            .enableDataUseAgreement(config.access.enableDataUseAgreement)
                            .enableEraCommons(config.access.enableEraCommons)
                            .firecloudURL(config.firecloud.baseUrl)
                            .unsafeAllowSelfBypass(config.access.unsafeAllowSelfBypass))
        }
}
