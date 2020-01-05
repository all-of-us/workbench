package org.pmiops.workbench.api;

import org.pmiops.workbench.model.ErrorResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link OfflineBillingApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link OfflineBillingApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public interface OfflineBillingApiDelegate {

    /**
     * @see OfflineBillingApi#billingProjectGarbageCollection
     */
    ResponseEntity<Void> billingProjectGarbageCollection();

    /**
     * @see OfflineBillingApi#bufferBillingProjects
     */
    ResponseEntity<Void> bufferBillingProjects();

    /**
     * @see OfflineBillingApi#checkFreeTierBillingUsage
     */
    ResponseEntity<Void> checkFreeTierBillingUsage();

    /**
     * @see OfflineBillingApi#cleanBillingBuffer
     */
    ResponseEntity<Void> cleanBillingBuffer();

    /**
     * @see OfflineBillingApi#syncBillingProjectStatus
     */
    ResponseEntity<Void> syncBillingProjectStatus();

}
