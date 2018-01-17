package org.pmiops.workbench.cohortreview;

import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class CohortReviewServiceImpl implements CohortReviewService {

    private CohortReviewDao cohortReviewDao;
    private CohortDao cohortDao;
    private ParticipantCohortStatusDao participantCohortStatusDao;
    private WorkspaceService workspaceService;
    private JdbcTemplate jdbcTemplate;

//    @PersistenceContext
//    private EntityManager entityManager;
//
//    @Value("${hibernate.jdbc.batch_size}")
//    private int batchSize;

    private static final Logger log = Logger.getLogger(CohortReviewServiceImpl.class.getName());

    @Autowired
    CohortReviewServiceImpl(CohortReviewDao cohortReviewDao,
                            CohortDao cohortDao,
                            ParticipantCohortStatusDao participantCohortStatusDao,
                            WorkspaceService workspaceService,
                            JdbcTemplate jdbcTemplate) {
        this.cohortReviewDao = cohortReviewDao;
        this.cohortDao = cohortDao;
        this.participantCohortStatusDao = participantCohortStatusDao;
        this.workspaceService = workspaceService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public CohortReviewServiceImpl() {
    }

    @Override
    public Cohort findCohort(long cohortId) {
        Cohort cohort = cohortDao.findOne(cohortId);
        if (cohort == null) {
            throw new NotFoundException(
                    String.format("Not Found: No Cohort exists for cohortId: %s", cohortId));
        }
        return cohort;
    }

    @Override
    public void validateMatchingWorkspace(String workspaceNamespace, String workspaceName, long workspaceId) {
        Workspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceName);
        if (workspace.getWorkspaceId() != workspaceId) {
            throw new NotFoundException(
                    String.format("Not Found: No workspace matching workspaceNamespace: %s, workspaceId: %s",
                            workspaceNamespace, workspaceName));
        }
    }

    @Override
    public CohortReview findCohortReview(Long cohortId, Long cdrVersionId) {
        CohortReview cohortReview = cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId, cdrVersionId);

        if (cohortReview == null) {
            throw new NotFoundException(
                    String.format("Not Found: Cohort Review does not exist for cohortId: %s, cdrVersionId: %s",
                            cohortId, cdrVersionId));
        }
        return cohortReview;
    }

    @Override
    public CohortReview findCohortReview(Long cohortReviewId) {
        CohortReview cohortReview = cohortReviewDao.findOne(cohortReviewId);

        if (cohortReview == null) {
            throw new NotFoundException(
                    String.format("Not Found: Cohort Review does not exist for cohortReviewId: %s",
                            cohortReviewId));
        }
        return cohortReview;
    }

    @Override
    public CohortReview saveCohortReview(CohortReview cohortReview) {
        return cohortReviewDao.save(cohortReview);
    }

    @Override
    @Transactional
    public void saveFullCohortReview(CohortReview cohortReview, List<ParticipantCohortStatus> participantCohortStatuses) {
        cohortReview = saveCohortReview(cohortReview);
        participantCohortStatuses = saveParticipantCohortStatuses(participantCohortStatuses);
    }

    @Override
    public List<ParticipantCohortStatus> saveParticipantCohortStatuses(List<ParticipantCohortStatus> participantCohortStatuses) {
        Statement statement = null;
        Connection connection = null;

        String sqlTemplate = "insert into participant_cohort_status(" +
                "birth_date, ethnicity_concept_id, gender_concept_id, race_concept_id, " +
                "status, cohort_review_id, participant_id) " +
                "values ('%s', %d, %d, %d, %d, %d, %d)";

        String nextInsert = ", ('%s', %d, %d, %d, %d, %d, %d)";
        int index = 0;
        int batchSize = 50;
        String sqlStatement = "";

        try {
            connection = jdbcTemplate.getDataSource().getConnection();
            statement = connection.createStatement();
            connection.setAutoCommit(true);

            for (ParticipantCohortStatus pcs : participantCohortStatuses) {
                sqlStatement = StringUtils.isEmpty(sqlStatement) ? sqlTemplate : sqlStatement + nextInsert;
                sqlStatement = String.format(sqlStatement,
                        pcs.getBirthDate().toString(),
                        pcs.getEthnicityConceptId(),
                        pcs.getGenderConceptId(),
                        pcs.getRaceConceptId(),
                        0,
                        pcs.getParticipantKey().getCohortReviewId(),
                        pcs.getParticipantKey().getParticipantId());

                if(++index % batchSize == 0) {
                    long start = System.currentTimeMillis();
                    statement.execute(sqlStatement);
                    long end = System.currentTimeMillis();

                    log.log(Level.INFO, "total time taken to insert the batch = " + (end - start) + " ms");
                    sqlStatement = "";
                }
            }

            if (!StringUtils.isEmpty(sqlStatement)) {
                long start = System.currentTimeMillis();
                statement.execute(sqlStatement);
                long end = System.currentTimeMillis();

                log.log(Level.INFO, "total time taken to insert the batch = " + (end - start) + " ms");
            }

        } catch (SQLException ex) {
            log.log(Level.INFO, "SQLException: " + ex.getMessage());
            throw new RuntimeException("SQLException: " + ex.getMessage(), ex);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    log.log(Level.INFO, "Problem closing prepared statement: " + e.getMessage());
                    throw new RuntimeException("SQLException: " + e.getMessage(), e);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.log(Level.INFO, "Problem closing connection: " + e.getMessage());
                    throw new RuntimeException("SQLException: " + e.getMessage(), e);
                }
            }
        }
        return participantCohortStatuses;
    }

    @Override
    public ParticipantCohortStatus saveParticipantCohortStatus(ParticipantCohortStatus participantCohortStatus) {
        return participantCohortStatusDao.save(participantCohortStatus);
    }

    @Override
    public ParticipantCohortStatus findParticipantCohortStatus(Long cohortReviewId, Long participantId) {
        ParticipantCohortStatus participantCohortStatus =
                participantCohortStatusDao.findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                        cohortReviewId,
                        participantId);
        if (participantCohortStatus == null) {
            throw new NotFoundException(
                    String.format("Not Found: Participant Cohort Status does not exist for cohortReviewId: %s, participantId: %s",
                            cohortReviewId, participantId));
        }
        return participantCohortStatus;
    }

    @Override
    public Slice<ParticipantCohortStatus> findParticipantCohortStatuses(Long cohortReviewId, PageRequest pageRequest) {
        return participantCohortStatusDao.findByParticipantKey_CohortReviewId(cohortReviewId, pageRequest);
    }
}
