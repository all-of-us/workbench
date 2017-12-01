package org.pmiops.workbench;

import com.blockscore.models.PaginatedResult;
import com.blockscore.models.Person;

/**
 * Encapsulate API for interfacing with Blockscore identity verification service.
 */
public interface BlockscoreService {
  public PaginatedResult<Person> listPeople();
}
