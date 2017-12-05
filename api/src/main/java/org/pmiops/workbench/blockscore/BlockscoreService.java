package org.pmiops.workbench.blockscore;

import com.blockscore.models.Address;
import com.blockscore.models.PaginatedResult;
import com.blockscore.models.Person;

/**
 * Encapsulate API for interfacing with Blockscore identity verification service.
 */
public interface BlockscoreService {
  public PaginatedResult<Person> listPeople();
  public Person createPerson(
    String firstName, String lastName, Address address, String dobString,
    String documentType, String documentNumber);
}
