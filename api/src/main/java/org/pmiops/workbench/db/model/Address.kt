package org.pmiops.workbench.db.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "address")
class Address {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "id")
    var id: Long = 0
    @get:Column(name = "street_address_1")
    var streetAddress1: String? = null
    @get:Column(name = "street_address_2")
    var streetAddress2: String? = null
    @get:Column(name = "zip_code")
    var zipCode: String? = null
    @get:Column(name = "city")
    var city: String? = null
    @get:Column(name = "state")
    var state: String? = null
    @get:Column(name = "country")
    var country: String? = null
    @get:ManyToOne
    @get:JoinColumn(name = "user_id")
    var user: User? = null
}
