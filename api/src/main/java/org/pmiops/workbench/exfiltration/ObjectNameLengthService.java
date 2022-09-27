package org.pmiops.workbench.exfiltration;

import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface ObjectNameLengthService {

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void calculateObjectNameLength(DbWorkspace workspace);
}
