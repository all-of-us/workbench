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

  private final CloudStorageService cloudStorageService;
  private String apiKey = null;

  @Autowired
  public BlockscoreServiceImpl(CloudStorageService cloudStorageService) {
    this.cloudStorageService = cloudStorageService;
    apiKey = cloudStorageService.readBlockscoreApiKey();
  }

  public PaginatedResult<Person> listPeople() {
    BlockscoreApiClient client = new BlockscoreApiClient(apiKey);
    return client.listPeople();
  }
}
