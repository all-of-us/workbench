package org.pmiops.workbench.google

import com.google.api.services.cloudresourcemanager.model.Project
import org.pmiops.workbench.db.model.User

/** Encapsulate Google APIs for interfacing with Google Cloud ResourceManager.  */
interface CloudResourceManagerService {
    fun getAllProjectsForUser(user: User): List<Project>
}
