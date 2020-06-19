package org.pmiops.workbench.shibboleth;

/**
 * Encapsulates API calls interacting with the Terra Shibboleth service. Although Shibboleth is part
 * of the Terra operational boundary, Shibboleth is not part of Terra's "Orchestration" API layer,
 * so it was more straightforward to model it separately from FireCloudService.
 */
public interface ShibbolethService {

  // Submits a JWT for verification and profile metadata update. The JWT should have come from
  // a browser-based login flow to the Shibboleth service. If successful, this call will
  // result in the calling user's Terra NihStatus being updated with the new linkage.
  void updateShibbolethToken(String jwt);
}
