package org.pmiops.workbench.firecloud;

import java.util.List;

/**
 * Encapsulate Firecloud API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface FireCloudService {

  /**
   * @return true if the user making the current request is enabled in FireCloud, false otherwise.
   */
  boolean isRequesterEnabledInFirecloud() throws ApiException;

  List<Entity> getEntitiesInWorkspace(String workspaceNamespace, String workspaceId,
      String entityType) throws ApiException;

  Entity createEntity(String workspaceNamespace, String workspaceId, Entity entity);

  Entity getEntity(String workspaceNamespace, String workspaceId, String entityType,
      String entityId);

  Entity updateEntity(String workspaceNamespace, String workspaceId, Entity entity);

  void deleteEntity(String workspaceNamespace, String workspaceId, String entityType,
      String entityId);
}
