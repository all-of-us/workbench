package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbFolderSyncTransfer;
import org.springframework.data.repository.CrudRepository;

public interface FolderSyncTransferDao extends CrudRepository<DbFolderSyncTransfer, Long> {
  DbFolderSyncTransfer findDbFolderSyncTransferByTransferJobName(String transferJobName);

  DbFolderSyncTransfer findFirstBySourceWorkspaceNamespaceOrderByStartedDesc(
      String sourceWorkspaceNamespace);
}
