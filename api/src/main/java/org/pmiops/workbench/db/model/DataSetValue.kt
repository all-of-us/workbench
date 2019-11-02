package org.pmiops.workbench.db.model

import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Table
import javax.persistence.Transient
import org.pmiops.workbench.model.Domain

@Embeddable
@Table(name = "data_set_cohort_id")
class DataSetValue {

    @get:Column(name = "domain_id")
    var domainId: String? = null
    @get:Column(name = "value")
    var value: String? = null

    val domainEnum: Domain
        @Transient
        get() = CommonStorageEnums.domainFromStorage(java.lang.Short.parseShort(domainId!!))

    constructor() {}

    constructor(domainId: String, value: String) {
        this.domainId = domainId
        this.value = value
    }

    constructor(dataSetValue: DataSetValue) {
        domainId = dataSetValue.domainId
        value = dataSetValue.value
    }

    fun equals(dataSetValue: DataSetValue): Boolean {
        return value == dataSetValue.value && domainEnum.equals(dataSetValue.domainEnum)
    }
}
