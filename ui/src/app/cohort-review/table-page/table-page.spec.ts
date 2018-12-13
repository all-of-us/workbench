import {NgRedux} from '@angular-redux/store';
import { APP_BASE_HREF } from '@angular/common';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {CohortReviewService} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';
import {CohortBuilderService} from '../../../generated';
import {CohortBuilderServiceStub} from '../../../testing/stubs/cohort-builder-service-stub';
import {ComboChartComponent} from '../../cohort-common/combo-chart/combo-chart.component';
import {CohortSearchActions} from '../../cohort-search/redux';
import {ClearButtonFilterComponent} from '../clearbutton-filter/clearbutton-filter.component';
import {MultiSelectFilterComponent} from '../multiselect-filter/multiselect-filter.component';
import {OverviewPage} from '../overview-page/overview-page';
import {ParticipantsChartsComponent} from '../participants-charts/participant-charts';
import {QueryCohortDefinitionComponent} from '../query-cohort-definition/query-cohort-definition.component';
import {QueryDescriptiveStatsComponent} from '../query-descriptive-stats/query-descriptive-stats.component';
import {QueryReportComponent} from '../query-report/query-report.component';
import {ReviewStateService} from '../review-state.service';
import {StatusFilterComponent} from '../status-filter/status-filter.component';
import {TablePage} from './table-page';



describe('TablePage', () => {
  let component: TablePage;
  let fixture: ComponentFixture<TablePage>;

  const activatedRouteStub = {
      snapshot: {
          data: {
            cohort: {
              name: '',
             },
              concepts: {
                  raceList: [],
                  genderList: [],
                  ethnicityList: [],
              }
          },
          pathFromRoot: [{data: {workspace: {cdrVersionId: 1}}}]
      },
      parent: {
          snapshot: {
              data: {
                  workspace: {
                      cdrVersionId: 1
                  },
                  cohort: {
                      name: ''
                  },
              },
            params: {
              ns: 'workspaceNamespace',
              wsid: 'workspaceId',
              cid: 1
            }
          },
        params: {
          ns: 'workspaceNamespace',
          wsid: 'workspaceId',
          cid: 1
        }
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
        {provide: CohortSearchActions},
        {provide: APP_BASE_HREF, useValue: '/'},
        {provide: CohortBuilderService, useValue: new CohortBuilderServiceStub()},
        {provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
        {provide: ActivatedRoute, useValue: activatedRouteStub},
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
      ],
    })
      .compileComponents();
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
