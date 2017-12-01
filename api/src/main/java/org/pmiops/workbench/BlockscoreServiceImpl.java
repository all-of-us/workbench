package org.pmiops.workbench;

import com.blockscore.models.PaginatedResult;
import com.blockscore.models.Person;
import com.blockscore.net.BlockscoreApiClient;
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
    if (apiKey == null) {
      apiKey = cloudStorageServiceProvider.get().readBlockscoreApiKey();
    }
    BlockscoreApiClient client = new BlockscoreApiClient(apiKey);
    return client.listPeople();
  }
}
