package org.pmiops.workbench.actionaudit.targetproperties

import io.opencensus.common.Timestamp
import org.pmiops.workbench.model.EgressEvent

/**
 * Action properties relating to high-egress events received by the Workbench. These
 * properties directly relate to values from an EgressEvent object instance.
 */
enum class EgressEventTargetProperty
constructor(
    override val propertyName: String,
    override val extractor: (EgressEvent) -> String?
) : ModelBackedTargetProperty<EgressEvent> {
    EGRESS_MIB("egress_mib",
            { it.egressMib?.toString() }),
    TIME_WINDOW_START("time_window_start",
            {
                if (it.timeWindowStart != null)
                    Timestamp.fromMillis(it.timeWindowStart).toString()
                else "[no time]"
            }),
    TIME_WINDOW_DURATION("time_window_duration",
            { it.timeWindowDuration?.toString() }),
    VM_NAME("vm_name", { it.vmName });
}

/**
 * A simple comment property relating to a high-egress event received by the Workbench.
 * This property is used to convey non-structured information about the inbound event, e.g.
 * when the event JSON failed to parse or when an associated workspace could not be found.
 */
enum class EgressEventCommentTargetProperty
constructor(override val propertyName: String) : SimpleTargetProperty {
    COMMENT("comment");
}
