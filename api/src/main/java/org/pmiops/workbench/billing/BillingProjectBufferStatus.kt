package org.pmiops.workbench.billing

enum class BillingProjectBufferStatus {
    CREATING, // Sent a request to FireCloud to create a BillingProject. Status of BillingProject is TBD
    ERROR, // Failed to create BillingProject
    AVAILABLE, // BillingProject is ready to be assigned to a user
    ASSIGNING, //  BillingProject is being assigned to a user
    ASSIGNED // BillingProject has been assigned to a user
}