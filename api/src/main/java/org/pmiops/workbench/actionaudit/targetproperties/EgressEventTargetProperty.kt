package org.pmiops.workbench.actionaudit.targetproperties

import io.opencensus.common.Timestamp
import org.pmiops.workbench.model.EgressEvent

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

enum class EgressEventCommentTargetProperty
constructor(override val propertyName: String) : SimpleTargetProperty {
    COMMENT("comment");
}
