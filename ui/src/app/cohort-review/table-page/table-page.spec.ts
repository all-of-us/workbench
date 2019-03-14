import {NgRedux} from '@angular-redux/store';
import { APP_BASE_HREF } from '@angular/common';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {ComboChartComponent} from 'app/cohort-common/combo-chart/combo-chart.component';
import {ClearButtonFilterComponent} from 'app/cohort-review/clearbutton-filter/clearbutton-filter.component';
import {MultiSelectFilterComponent} from 'app/cohort-review/multiselect-filter/multiselect-filter.component';
import {OverviewPage} from 'app/cohort-review/overview-page/overview-page';
import {ParticipantsChartsComponent} from 'app/cohort-review/participants-charts/participant-charts';
import {QueryCohortDefinitionComponent} from 'app/cohort-review/query-cohort-definition/query-cohort-definition.component';
import {QueryDescriptiveStatsComponent} from 'app/cohort-review/query-descriptive-stats/query-descriptive-stats.component';
import {QueryReportComponent} from 'app/cohort-review/query-report/query-report.component';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {StatusFilterComponent} from 'app/cohort-review/status-filter/status-filter.component';
import {CohortSearchActions} from 'app/cohort-search/redux';
import {CdrVersionStorageService} from 'app/services/cdr-version-storage.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentCohortStore, currentWorkspaceStore} from 'app/utils/navigation';
import {DataAccessLevel} from 'generated';
import {CohortBuilderApi, CohortReviewApi} from 'generated/fetch';
import {NgxPopperModule} from 'ngx-popper';
import {CdrVersionStorageServiceStub} from 'testing/stubs/cdr-version-storage-service-stub';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {CohortReviewServiceStub, cohortReviewStub} from 'testing/stubs/cohort-review-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspace-storage-service-stub';
import {TablePage} from './table-page';



describe('TablePage', () => {
  let component: TablePage;
  let fixture: ComponentFixture<TablePage>;

  beforeEach(async(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());

    TestBed.configureTestingModule({
      declarations: [
        ClearButtonFilterComponent,
        MultiSelectFilterComponent,
        TablePage,
        StatusFilterComponent,
        OverviewPage,
        ComboChartComponent,
        ParticipantsChartsComponent,
        QueryReportComponent,
        QueryCohortDefinitionComponent,
        QueryDescriptiveStatsComponent
      ],
      imports: [ClarityModule,
        ReactiveFormsModule,
        RouterTestingModule,
        NgxPopperModule,
        NgxChartsModule],
      providers: [
        {provide: NgRedux},
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
        {provide: CohortSearchActions},
        {provide: APP_BASE_HREF, useValue: '/'},
      ],
    })
      .compileComponents();
    currentCohortStore.next({
      name: '',
      criteria: '',
      type: '',
    });
    cohortReviewStore.next(cohortReviewStub);
    currentWorkspaceStore.next(workspaceDataStub);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TablePage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
