package org.pmiops.workbench.db.model

import java.sql.Timestamp
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "page_visit")
class PageVisit {

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "page_visit_id")
    var pageVisitId: Long = 0
    @get:Column(name = "page_id")
    var pageId: String? = null
    @get:Column(name = "first_visit")
    var firstVisit: Timestamp? = null
    @get:ManyToOne
    @get:JoinColumn(name = "user_id")
    var user: User? = null
}
