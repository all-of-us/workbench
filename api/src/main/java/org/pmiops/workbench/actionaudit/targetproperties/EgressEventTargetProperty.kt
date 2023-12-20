package org.pmiops.workbench.actionaudit.targetproperties

import io.opencensus.common.Timestamp
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.model.DbEgressEvent
import org.pmiops.workbench.model.SumologicEgressEvent

/**
 * Action properties relating to high-egress events received by the Workbench. These
 * properties directly relate to values from an EgressEvent object instance.
 */
enum class EgressEventTargetProperty
    constructor(
        override val propertyName: String,
        override val extractor: (SumologicEgressEvent) -> String?,
    ) : ModelBackedTargetProperty<SumologicEgressEvent> {
        EGRESS_MIB(
            "egress_mib",
            { it.egressMib?.toString() },
        ),
        TIME_WINDOW_START(
            "time_window_start",
            {
                if (it.timeWindowStart != null) {
                    Timestamp.fromMillis(it.timeWindowStart).toString()
                } else {
                    "[no time]"
                }
            },
        ),
        TIME_WINDOW_DURATION(
            "time_window_duration",
            { it.timeWindowDuration?.toString() },
        ),
        VM_NAME("vm_prefix", { it.vmPrefix }),
    }

/**
 * Action properties relating to a stored high-egress event within the workbench.
 */
enum class DbEgressEventTargetProperty
    constructor(
        override val propertyName: String,
        override val extractor: (DbEgressEvent) -> String?,
    ) : ModelBackedTargetProperty<DbEgressEvent> {
        EGRESS_EVENT_ID(
            "egress_event_id",
            { it.egressEventId.toString() },
        ),
        USER_ID("user_id", { it.user?.userId.toString() }),
        CREATION_TIME("creation_time", { it.creationTime?.toString() }),
        STATUS("status", { it.status?.toString() }),
    }

/**
 * Action properties describing the specific policy escalation automatically applied in response to
 * a high-egress event within the workbench.
 */
enum class EgressEscalationTargetProperty
    constructor(
        override val propertyName: String,
        override val extractor: (WorkbenchConfig.EgressAlertRemediationPolicy.Escalation) -> String?,
    ) : ModelBackedTargetProperty<WorkbenchConfig.EgressAlertRemediationPolicy.Escalation> {
        REMEDIATION(
            "remediation",
            {
                if (it.disableUser != null) {
                    "disable_user"
                } else {
                    "suspend_compute"
                }
            },
        ),
        SUSPEND_COMPUTE_DURATION_MIN("suspend_duration", { it.suspendCompute?.durationMinutes?.toString() }),
    }

/**
 * A simple comment property relating to a high-egress event received by the Workbench.
 * This property is used to convey non-structured information about the inbound event, e.g.
 * when the event JSON failed to parse or when an associated workspace could not be found.
 */
enum class EgressEventCommentTargetProperty
    constructor(override val propertyName: String) : SimpleTargetProperty {
        COMMENT("comment"),
    }
