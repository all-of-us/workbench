package org.pmiops.workbench.api

import javax.inject.Provider
import org.pmiops.workbench.config.FeaturedWorkspacesConfig
import org.pmiops.workbench.model.FeaturedWorkspacesConfigResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class FeaturedWorkspacesController @Autowired
internal constructor(private val configProvider: Provider<FeaturedWorkspacesConfig>) : FeaturedWorkspacesConfigApiDelegate {

    val featuredWorkspacesConfig: ResponseEntity<FeaturedWorkspacesConfigResponse>
        get() {
            val fwConfig = configProvider.get()
            return ResponseEntity.ok(
                    FeaturedWorkspacesConfigResponse().featuredWorkspacesList(fwConfig.featuredWorkspaces))
        }
}
