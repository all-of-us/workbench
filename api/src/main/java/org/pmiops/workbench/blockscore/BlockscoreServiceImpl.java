package org.pmiops.workbench.blockscore;

import com.blockscore.models.Address;
import com.blockscore.models.PaginatedResult;
import com.blockscore.models.Person;
import com.blockscore.net.BlockscoreApiClient;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.inject.Provider;
import org.pmiops.workbench.google.CloudStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlockscoreServiceImpl implements BlockscoreService {

  private final Provider<CloudStorageService> cloudStorageServiceProvider;
  private String apiKey = null;

  @Autowired
  public BlockscoreServiceImpl(Provider<CloudStorageService> cloudStorageServiceProvider) {
    this.cloudStorageServiceProvider = cloudStorageServiceProvider;
  }

  public PaginatedResult<Person> listPeople() {
    return getClient().listPeople();
  }

  public Person createPerson(
      String firstName, String lastName, Address address, String dobString,
      String documentType, String documentNumber) {
    Person.Builder person = new Person.Builder(getClient());
    person
      .setFirstName(firstName).setLastName(lastName).setAddress(address)
      .setDocumentType(documentType).setDocumentValue(documentNumber);
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    try {
      person.setDateOfBirth(formatter.parse(dobString));
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    return person.create();
  }

  BlockscoreApiClient getClient() {
    if (apiKey == null) {
      apiKey = cloudStorageServiceProvider.get().readBlockscoreApiKey();
    }
    return new BlockscoreApiClient(apiKey);
  }
}
