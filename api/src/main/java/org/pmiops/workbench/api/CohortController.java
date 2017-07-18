package org.pmiops.workbench.api;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.pmiops.workbench.model.Cohort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortController implements CohortsApi {

  @Override
  public ResponseEntity<List<Cohort>> getCohortsInWorkspace(String workspaceNamespace,
      String workspaceId) {
    Cohort cohort1 = new Cohort();
    cohort1.setId("id1");
    cohort1.setName("name1");
    cohort1.setCriteria("criteria1");
    cohort1.setType("type1");

    Cohort cohort2 = new Cohort();
    cohort2.setId("id2");
    cohort2.setName("name2");
    cohort2.setCriteria("criteria2");
    cohort2.setType("type2");

    return ResponseEntity.ok(ImmutableList.of(cohort1, cohort2));
  }
}
