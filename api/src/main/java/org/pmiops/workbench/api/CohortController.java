package org.pmiops.workbench.api;

import com.google.api.services.oauth2.model.Userinfoplus;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.firecloud.Entity.EntityTypes;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortController implements CohortsApi {

  private final WorkspaceDao workspaceDao;
  private final Provider<Userinfoplus> userInfoProvider;
  private final Clock clock;

  @Autowired
  CohortController(WorkspaceDao workspaceDao, Provider<Userinfoplus> userInfoProvider,
      Clock clock) {
    this.workspaceDao = workspaceDao;
    this.userInfoProvider = userInfoProvider;
    this.clock = clock;
  }

  @Override
  public ResponseEntity<List<Cohort>> getCohortsInWorkspace(String workspaceNamespace,
      String workspaceId) {
    return null;
  }
}
