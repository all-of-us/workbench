package org.pmiops.workbench.workspaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.firecloud.FireCloudConfig.END_USER_API_CLIENT;
import static org.pmiops.workbench.firecloud.FireCloudConfig.END_USER_WORKSPACE_API;
import static org.pmiops.workbench.utils.TestMockFactory.createDbWorkspaceStub;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;
import static org.pmiops.workbench.utils.TestMockFactory.createWorkspace;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.CdrDataSource;
import org.pmiops.workbench.cdr.DbParams;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WebMvcConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.Params;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceDetails;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.interceptors.AuthInterceptor;
import org.pmiops.workbench.interceptors.RequestTimeMetricInterceptor;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;

@Provider("AoURWAPI_Workspaces")
@PactFolder("src/test/resources/pacts")
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
public class ProductPactProviderTest {
  @MockBean private DataSource dataSource;
@MockBean private DbParams cdrParams;
@MockBean private CdrDataSource cdrDataSource;
//@Autowired private MockMvc mockMvc;

  @MockBean WebMvcConfig webMvcConfig;
  @MockBean(name = "params")
  Params params;

  // Mockmvc
  @MockBean private AuthInterceptor authInterceptor;
  @MockBean private RequestTimeMetricInterceptor requestTimeMetricInterceptor;
  //
  // Cpontroller Dependencies
  @Autowired private CdrVersionDao cdrVersionDao;
  @MockBean private FreeTierBillingService freeTierBillingService;
  @MockBean private IamService iamService;
  @MockBean private javax.inject.Provider<DbUser> userProvider;
  @MockBean private javax.inject.Provider<WorkbenchConfig> workbenchConfigProvider;
  @MockBean private TaskQueueService taskQueueService;
  @MockBean private UserDao userDao;
  @MockBean private WorkspaceAuditor workspaceAuditor;
  @MockBean private WorkspaceAuthService workspaceAuthService;
  @MockBean private WorkspaceDao workspaceDao;

  @MockBean private UserAuthentication userAuthentication;
  @MockBean private WorkbenchConfig workbenchConfig;
  @MockBean(name = END_USER_API_CLIENT) private ApiClient apiClient;

  @MockBean private javax.inject.Provider<WorkspacesApi> prototypeFactoryStub;
  @MockBean(name = END_USER_WORKSPACE_API) private WorkspacesApi mockWorkspacesApi;
  @Autowired private AccessTierDao accessTierDao;

  @BeforeEach
  public void setUp(PactVerificationContext context) throws ApiException {
    context.setTarget(new HttpTestTarget("localhost", serverPort));

  }

  @LocalServerPort private int serverPort;

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider.class)
  void verifyPact(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @State("User has workspaces")
  void toProductsExistState() throws ApiException {
    ArrayList<FirecloudWorkspaceResponse> firecloudWorkspaceResponseList =
        new ArrayList<>();

    FirecloudWorkspaceResponse firstFirecloudWorkspaceResponse = new FirecloudWorkspaceResponse();
    FirecloudWorkspaceResponse secondFirecloudWorkspaceResponse = new FirecloudWorkspaceResponse();
    FirecloudWorkspaceResponse thirdFirecloudWorkspaceResponse = new FirecloudWorkspaceResponse();

    FirecloudWorkspaceDetails firstFirecloudWorkspaceDetails = new FirecloudWorkspaceDetails();
    FirecloudWorkspaceDetails secondFirecloudWorkspaceDetails = new FirecloudWorkspaceDetails();
    FirecloudWorkspaceDetails thirdFirecloudWorkspaceDetails = new FirecloudWorkspaceDetails();

    firstFirecloudWorkspaceDetails.setNamespace("defaultNamespaceA");
    firstFirecloudWorkspaceDetails.setWorkspaceId("1337");
    firstFirecloudWorkspaceDetails.setName("1A");
    firstFirecloudWorkspaceDetails.setBucketName("");
    firstFirecloudWorkspaceDetails.setCreatedBy("");
    firstFirecloudWorkspaceResponse.setWorkspace(firstFirecloudWorkspaceDetails);
    firstFirecloudWorkspaceResponse.setAccessLevel(WorkspaceAccessLevel.WRITER.toString());


    secondFirecloudWorkspaceDetails.setNamespace("defaultNamespaceB");
    secondFirecloudWorkspaceDetails.setWorkspaceId("1338");
    secondFirecloudWorkspaceDetails.setName("1B");
    secondFirecloudWorkspaceDetails.setBucketName("");
    secondFirecloudWorkspaceDetails.setCreatedBy("");
    secondFirecloudWorkspaceResponse.setWorkspace(secondFirecloudWorkspaceDetails);
    secondFirecloudWorkspaceResponse.setAccessLevel(WorkspaceAccessLevel.WRITER.toString());

    thirdFirecloudWorkspaceDetails.setNamespace("defaultNamespaceC");
    thirdFirecloudWorkspaceDetails.setWorkspaceId("1339");
    thirdFirecloudWorkspaceDetails.setName("1C");
    thirdFirecloudWorkspaceDetails.setBucketName("");
    thirdFirecloudWorkspaceDetails.setCreatedBy("");
    thirdFirecloudWorkspaceResponse.setWorkspace(thirdFirecloudWorkspaceDetails);
    thirdFirecloudWorkspaceResponse.setAccessLevel(WorkspaceAccessLevel.WRITER.toString());

    firecloudWorkspaceResponseList.add(firstFirecloudWorkspaceResponse);
    firecloudWorkspaceResponseList.add(secondFirecloudWorkspaceResponse);
    firecloudWorkspaceResponseList.add(thirdFirecloudWorkspaceResponse);


    DbWorkspace firstWorkspace = createDbWorkspaceStub(createWorkspace("defaultNamespaceDDDDB","defaultWorkspaceA"),5);
    DbWorkspace secondWorkspace = createDbWorkspaceStub(createWorkspace("defaultNamespaceDDDDB","defaultWorkspaceB"),5);
    DbWorkspace thirdWorkspace = createDbWorkspaceStub(createWorkspace("defaultNamespaceDDDDB","defaultWorkspaceC"),5);

    firstWorkspace.setAncestry(false);
    firstWorkspace.setAnticipatedFindings("");
    firstWorkspace.setBillingAccountName("billing-account");
    firstWorkspace.setControlSet(false);
    firstWorkspace.setSocialBehavioral(false);
    firstWorkspace.setScientificApproach("");
    firstWorkspace.setCommercialPurpose(false);
    firstWorkspace.setReasonForAllOfUs("");
    firstWorkspace.setReviewRequested(false);
    firstWorkspace.setOtherPurposeDetails("");
    firstWorkspace.setEthics(true);
    firstWorkspace.setDiseaseFocusedResearch(false);
    firstWorkspace.setMethodsDevelopment(false);
    firstWorkspace.setIntendedStudy("");
    DbCdrVersion firstRegisteredCdr = createDefaultCdrVersion(-1);
    firstWorkspace.setCdrVersion(firstRegisteredCdr);
    firstWorkspace.setFirecloudName("Coral");
    firstWorkspace.setFirecloudUuid("1337");
    firstWorkspace.setCreationTime(new Timestamp(1675382400000L)); //February 3rd 2023 00:00:00
    firstWorkspace.setLastModifiedTime(new Timestamp(1675382400000L)); //February 3rd 2023 00:00:00
    HashSet<SpecificPopulationEnum> firstSpecificPopulations = new HashSet<>();
    firstSpecificPopulations.add(SpecificPopulationEnum.RACE_NHPI);
    firstSpecificPopulations.add(SpecificPopulationEnum.AGE_OLDER_MORE_THAN_75);
    firstWorkspace.setSpecificPopulationsEnum(firstSpecificPopulations);
    firstWorkspace.setAdminLockedReason("");
    firstWorkspace.setVersion(0);
    firstWorkspace.setGoogleProject("");
    firstWorkspace.setLastModifiedBy("");
    firstWorkspace.setApproved(false);
    firstWorkspace.setDisseminateResearchOther("");
    firstWorkspace.setOtherPopulationDetails("");
    firstWorkspace.setTimeRequested(new Timestamp(1675382400000L)); //February 3rd 2023 00:00:00

    secondWorkspace.setAncestry(false);
    secondWorkspace.setAnticipatedFindings("");
    secondWorkspace.setBillingAccountName("billing-account");
    secondWorkspace.setControlSet(false);
    secondWorkspace.setSocialBehavioral(false);
    secondWorkspace.setScientificApproach("");
    secondWorkspace.setCommercialPurpose(false);
    secondWorkspace.setReasonForAllOfUs("");
    secondWorkspace.setReviewRequested(false);
    secondWorkspace.setOtherPurposeDetails("");
    secondWorkspace.setEthics(true);
    secondWorkspace.setDiseaseFocusedResearch(false);
    secondWorkspace.setMethodsDevelopment(false);
    secondWorkspace.setIntendedStudy("");
    DbCdrVersion secondRegisteredCdr = createDefaultCdrVersion(-1);
    secondWorkspace.setCdrVersion(secondRegisteredCdr);
    secondWorkspace.setFirecloudName("Coral");
    secondWorkspace.setFirecloudUuid("1338");
    secondWorkspace.setCreationTime(new Timestamp(1675382400000L)); //February 3rd 2023 00:00:00
    secondWorkspace.setLastModifiedTime(new Timestamp(1675382400000L)); //February 3rd 2023 00:00:00
    HashSet<SpecificPopulationEnum> secondSpecificPopulations = new HashSet<>();
    secondSpecificPopulations.add(SpecificPopulationEnum.RACE_NHPI);
    secondSpecificPopulations.add(SpecificPopulationEnum.AGE_OLDER_MORE_THAN_75);
    secondWorkspace.setSpecificPopulationsEnum(secondSpecificPopulations);
    secondWorkspace.setAdminLockedReason("");
    secondWorkspace.setVersion(0);
    secondWorkspace.setGoogleProject("");
    secondWorkspace.setLastModifiedBy("");
    secondWorkspace.setApproved(false);
    secondWorkspace.setDisseminateResearchOther("");
    secondWorkspace.setOtherPopulationDetails("");
    secondWorkspace.setTimeRequested(new Timestamp(1675382400000L));

    thirdWorkspace.setAncestry(false);
    thirdWorkspace.setAnticipatedFindings("");
    thirdWorkspace.setBillingAccountName("billing-account");
    thirdWorkspace.setControlSet(false);
    thirdWorkspace.setSocialBehavioral(false);
    thirdWorkspace.setScientificApproach("");
    thirdWorkspace.setCommercialPurpose(false);
    thirdWorkspace.setReasonForAllOfUs("");
    thirdWorkspace.setReviewRequested(false);
    thirdWorkspace.setOtherPurposeDetails("");
    thirdWorkspace.setEthics(true);
    thirdWorkspace.setDiseaseFocusedResearch(false);
    thirdWorkspace.setMethodsDevelopment(false);
    thirdWorkspace.setIntendedStudy("");
    DbCdrVersion thirdRegisteredCdr = createDefaultCdrVersion(-1);
    thirdWorkspace.setCdrVersion(thirdRegisteredCdr);
    thirdWorkspace.setFirecloudName("Coral");
    thirdWorkspace.setFirecloudUuid("1339");
    thirdWorkspace.setCreationTime(new Timestamp(1675382400000L)); //February 3rd 2023 00:00:00
    thirdWorkspace.setLastModifiedTime(new Timestamp(1675382400000L)); //February 3rd 2023 00:00:00
    HashSet<SpecificPopulationEnum> thirdSpecificPopulations = new HashSet<>();
    thirdSpecificPopulations.add(SpecificPopulationEnum.RACE_NHPI);
    thirdSpecificPopulations.add(SpecificPopulationEnum.AGE_OLDER_MORE_THAN_75);
    thirdWorkspace.setSpecificPopulationsEnum(thirdSpecificPopulations);
    thirdWorkspace.setAdminLockedReason("");
    thirdWorkspace.setVersion(0);
    thirdWorkspace.setGoogleProject("");
    thirdWorkspace.setLastModifiedBy("");
    thirdWorkspace.setApproved(false);
    thirdWorkspace.setDisseminateResearchOther("");
    thirdWorkspace.setOtherPopulationDetails("");
    thirdWorkspace.setTimeRequested(new Timestamp(1675382400000L));

    when(prototypeFactoryStub.get()).thenReturn(mockWorkspacesApi);
    when(mockWorkspacesApi.listWorkspaces(any())).thenReturn(firecloudWorkspaceResponseList);
    when(workspaceDao.findActiveByFirecloudUuidIn(any()))
        .thenReturn(new ArrayList<>(Arrays.asList(firstWorkspace, secondWorkspace, thirdWorkspace)));
  }
}
