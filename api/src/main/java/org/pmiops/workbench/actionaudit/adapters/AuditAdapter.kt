package org.pmiops.workbench.actionaudit.adapters

/**
 * The AuditAdapter types serve to insulate Controllers and other classes being instrumented
 * from code dealing with property extraction and forming ActionAuditEvents. A primary driver
 * for the design of this system is that audit failures do not propagate and break instrumented
 * code.
 */
interface AuditAdapter<T> {

}
