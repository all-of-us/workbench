package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.utils.WorkspaceMapper;
import org.pmiops.workbench.utils.WorkspaceMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class WorkspaceMapperTest {

    private static final String FIRECLOUD_NAMESPACE = "aou-xxxxxxx";
    private static final String CREATOR_EMAIL = "ojc@verily.biz";
    private static final long CREATOR_USER_ID =  101L;
    private static final long WORKSPACE_DB_ID = 222L;
    private static final int WORKSPACE_VERSION = 2;
    public static final String WORKSPACE_AOU_NAME = "studyallthethings";
    private static final String WORKSPACE_FIRECLOUD_NAME = "aaaa-bbbb-cccc-dddd";
    private static final Short DATA_ACCESS_LEVEL_ORDINAL = CommonStorageEnums
        .dataAccessLevelToStorage(DataAccessLevel.REGISTERED);
    private Workspace sourceClientWorkspace;
    private DbWorkspace sourceDbWorkspace;
    private org.pmiops.workbench.firecloud.model.Workspace sourceFirecloudWorkspace;
    @Autowired private WorkspaceMapper workspaceMapper;
    @Autowired private WorkspaceDao mockWorkspaceDao;

    @TestConfiguration
    @Import({WorkspaceMapperImpl.class})
    @MockBean({WorkspaceDao.class})
    static class Configuration {}

    @Before
    public void setUp() {
        sourceFirecloudWorkspace = new org.pmiops.workbench.firecloud.model.Workspace()
            .workspaceId(Long.toString(CREATOR_USER_ID))
            .createdBy(CREATOR_EMAIL)
            .namespace(FIRECLOUD_NAMESPACE)
            .name(WORKSPACE_FIRECLOUD_NAME);

        final DbUser creatorUser = new DbUser();
        creatorUser.setEmail(CREATOR_EMAIL);
        creatorUser.setDataAccessLevel(DATA_ACCESS_LEVEL_ORDINAL);
        creatorUser.setUserId(CREATOR_USER_ID);

        sourceDbWorkspace = new DbWorkspace();
        sourceDbWorkspace.setWorkspaceId(WORKSPACE_DB_ID);
        sourceDbWorkspace.setVersion(WORKSPACE_VERSION);
        sourceDbWorkspace.setName(WORKSPACE_AOU_NAME);
        sourceDbWorkspace.setFirecloudName(WORKSPACE_FIRECLOUD_NAME);
        sourceDbWorkspace.setDataAccessLevel(DATA_ACCESS_LEVEL_ORDINAL);
        sourceDbWorkspace.setCreator(creatorUser);
    }

    @Test
    public void testConvertsSimpleWorkspace() {

        final Workspace outputWorkspace = workspaceMapper.toApiWorkspace(
            sourceDbWorkspace, sourceFirecloudWorkspace);
        assertThat(outputWorkspace.getId()).isEqualTo(sourceFirecloudWorkspace.getName());
    }
}
