package org.pmiops.workbench.cohorts;

import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;

@Service
public class CohortFactoryImpl implements CohortFactory {

    private final Clock clock;

    @Autowired
    public CohortFactoryImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Cohort createCohort(org.pmiops.workbench.model.Cohort apiCohort, User creator, long workspaceId) {
        return createCohort(
                apiCohort.getDescription(),
                apiCohort.getName(),
                apiCohort.getType(),
                apiCohort.getCriteria(),
                creator,
                workspaceId
        );
    }

    @Override
    public Cohort duplicateCohort(Cohort from, User creator) {
        return createCohort(
                from.getDescription(),
                from.getName(),
                from.getType(),
                from.getCriteria(),
                creator,
                from.getWorkspaceId()
        );
    }

    private Cohort createCohort(String desc, String name, String type, String criteria, User creator, long workspaceId) {
        Timestamp now = new Timestamp(clock.instant().toEpochMilli());
        Cohort cohort = new Cohort();

        cohort.setDescription(desc);
        cohort.setName(name);
        cohort.setType(type);
        cohort.setCriteria(criteria);
        cohort.setCreationTime(now);
        cohort.setLastModifiedTime(now);
        cohort.setVersion(1);
        cohort.setCreator(creator);
        cohort.setWorkspaceId(workspaceId);

        return cohort;
    }

}
