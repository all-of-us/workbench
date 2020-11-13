package org.pmiops.workbench.reporting

import org.pmiops.workbench.model.ReportingSnapshot

/*
* This service encapsulate the business of obtaining the current analytics snapshot
* from the application MySQL DB and third party sources (e.g. Terra).
*/
interface ReportingSnapshotService {
    fun takeSnapshot(): ReportingSnapshot?
}
