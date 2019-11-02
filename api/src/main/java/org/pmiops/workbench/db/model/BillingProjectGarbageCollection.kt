package org.pmiops.workbench.db.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "billing_project_garbage_collection")
class BillingProjectGarbageCollection {
    @get:Id
    @get:Column(name = "firecloud_project_name")
    var fireCloudProjectName: String? = null
    @get:Column(name = "owner")
    var owner: String? = null
}
