import {NgRedux} from '@angular-redux/store';
import { APP_BASE_HREF } from '@angular/common';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
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
import {currentCohortStore} from 'app/utils/navigation';
import {CohortBuilderService} from 'generated';
import {CohortReviewService, DataAccessLevel} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import {CdrVersionStorageServiceStub} from 'testing/stubs/cdr-version-storage-service-stub';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {CohortReviewServiceStub, cohortReviewStub} from 'testing/stubs/cohort-review-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {TablePage} from './table-page';



describe('TablePage', () => {
  let component: TablePage;
  let fixture: ComponentFixture<TablePage>;

  const activatedRouteStub = {
    snapshot: {
      data: {
        concepts: {
          raceList: [],
          genderList: [],
          ethnicityList: [],
        },
      },
    },
  };
  let route;
  beforeEach(async(() => {

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
        {provide: CohortReviewService},
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
        {provide: CohortBuilderService, useValue: new CohortBuilderServiceStub()},
        {provide: ActivatedRoute, useValue: activatedRouteStub},
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
      ],
    })
      .compileComponents();
    currentCohortStore.next({
      name: '',
      criteria: '',
      type: '',
    });
    cohortReviewStore.next(cohortReviewStub);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TablePage);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
