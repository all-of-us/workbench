package org.pmiops.workbench.exfiltration;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface ObjectNameLengthService {

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void calculateObjectNameLength();
}
