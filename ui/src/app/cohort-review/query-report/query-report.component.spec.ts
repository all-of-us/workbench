import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {ComboChartComponent} from 'app/cohort-common/combo-chart/combo-chart.component';
import {OverviewPage} from 'app/cohort-review/overview-page/overview-page';
import {ParticipantsChartsComponent} from 'app/cohort-review/participants-charts/participants-charts';
import {QueryCohortDefinitionComponent} from 'app/cohort-review/query-cohort-definition/cohort-definition.component';
import {QueryDescriptiveStatsComponent} from 'app/cohort-review/query-descriptive-stats/query-descriptive-stats.component';
import {QueryReportComponent} from 'app/cohort-review/query-report/query-report.component';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {CdrVersionStorageService} from 'app/services/cdr-version-storage.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentCohortStore, currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {DataAccessLevel, WorkspaceAccessLevel} from 'generated';
import {CohortBuilderApi, CohortReviewApi} from 'generated/fetch';
import {NgxPopperModule} from 'ngx-popper';
import {CdrVersionStorageServiceStub} from 'testing/stubs/cdr-version-storage-service-stub';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {CohortReviewServiceStub, cohortReviewStub} from 'testing/stubs/cohort-review-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';





describe('QueryReportComponent', () => {
  let component: QueryReportComponent;
  let fixture: ComponentFixture<QueryReportComponent>;

  const criteria = {
    includes: [{
      items: [{
        type: 'PM',
        modifiers: [{
          name: 'AGE_AT_EVENT',
          operands: ['60', '30'],
          operator: 'GREATER_THAN_OR_EQUAL_TO'
        }],
        searchParameters: [{
          name: 'Hypotensive (Systolic <= 90 / Diastolic <= 60)',
          type: 'PM'
        }]
      }]
    }],
    excludes: []
  };

  beforeEach(async(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    TestBed.configureTestingModule({
      declarations: [
        ComboChartComponent,
        OverviewPage,
        ParticipantsChartsComponent,
        QueryReportComponent,
        QueryCohortDefinitionComponent,
        QueryDescriptiveStatsComponent
      ],
      imports: [
        ClarityModule,
        NgxChartsModule,
        NgxPopperModule,
      ],
      providers: [
        { provide: CdrVersionStorageService,
          useValue: new CdrVersionStorageServiceStub({
            defaultCdrVersionId: WorkspacesServiceStub.stubWorkspace().cdrVersionId,
            items: [{
              name: 'cdr1',
              cdrVersionId: WorkspacesServiceStub.stubWorkspace().cdrVersionId,
              dataAccessLevel: DataAccessLevel.Registered,
              creationTime: 0
            }]
          })},
      ]
    })
      .compileComponents();
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      cdrVersionId: '1',
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
    currentCohortStore.next({
      name: '',
      criteria: JSON.stringify(criteria),
      type: '',
    });
    urlParamsStore.next({
      ns: 'workspaceNamespace',
      wsid: 'workspaceId',
      cid: 1
    });
    cohortReviewStore.next(cohortReviewStub);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(QueryReportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
