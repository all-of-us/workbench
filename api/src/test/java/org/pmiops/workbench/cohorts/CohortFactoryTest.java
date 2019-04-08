package org.pmiops.workbench.cohorts;

import org.junit.Before;
import org.junit.Test;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.User;

import java.time.Clock;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CohortFactoryTest {

    private CohortFactory cohortFactory;

    @Before
    public void setUp() {
        cohortFactory = new CohortFactoryImpl(Clock.systemUTC());
    }

    @Test
    public void createCohort() {
        org.pmiops.workbench.model.Cohort apiCohort = new org.pmiops.workbench.model.Cohort();
        apiCohort.setDescription("desc");
        apiCohort.setName("name");
        apiCohort.setType("type");
        apiCohort.setCriteria("criteria");

        User user = mock(User.class);

        long workspaceId = 1l;

        Cohort dbCohort = cohortFactory.createCohort(apiCohort, user, workspaceId);

        assertThat(dbCohort.getDescription()).isEqualTo(apiCohort.getDescription());
        assertThat(dbCohort.getName()).isEqualTo(apiCohort.getName());
        assertThat(dbCohort.getType()).isEqualTo(apiCohort.getType());
        assertThat(dbCohort.getCriteria()).isEqualTo(apiCohort.getCriteria());
        assertThat(dbCohort.getCreator()).isSameAs(user);
        assertThat(dbCohort.getWorkspaceId()).isEqualTo(workspaceId);
    }

    @Test
    public void duplicateCohort() {
        Cohort originalCohort = new Cohort();
        originalCohort.setDescription("desc");
        originalCohort.setName("name");
        originalCohort.setType("type");
        originalCohort.setCriteria("criteria");
        originalCohort.setWorkspaceId(1l);
        originalCohort.setCohortReviews(Collections.singleton(mock(CohortReview.class)));

        User user = mock(User.class);
        Cohort dbCohort = cohortFactory.duplicateCohort("new name", originalCohort, user);

        assertThat(dbCohort.getDescription()).isEqualTo(originalCohort.getDescription());
        assertThat(dbCohort.getName()).isEqualTo("new name");
        assertThat(dbCohort.getType()).isEqualTo(originalCohort.getType());
        assertThat(dbCohort.getCriteria()).isEqualTo(originalCohort.getCriteria());
        assertThat(dbCohort.getCreator()).isSameAs(user);
        assertThat(dbCohort.getWorkspaceId()).isEqualTo(originalCohort.getWorkspaceId());
        assertThat(dbCohort.getCohortReviews()).isNull();
    }

}
