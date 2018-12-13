import {NgRedux} from '@angular-redux/store';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {CohortBuilderService, CohortReviewService} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';
import {CohortReviewServiceStub} from '../../../testing/stubs/cohort-review-service-stub';
import {ComboChartComponent} from '../../cohort-common/combo-chart/combo-chart.component';
import {OverviewPage} from '../overview-page/overview-page';
import {ParticipantsChartsComponent} from '../participants-charts/participant-charts';
import {ReviewStateService} from '../review-state.service';
import {QueryCohortDefinitionComponent} from "../query-cohort-definition/query-cohort-definition.component";
import {QueryDescriptiveStatsComponent} from "../query-descriptive-stats/query-descriptive-stats.component";
import { QueryReportComponent } from './query-report.component';

describe('QueryReportComponent', () => {
  let component: QueryReportComponent;
  let fixture: ComponentFixture<QueryReportComponent>;

  const criteria = {
    includes: [{
      items: [{
        type: 'PM',
        modifiers:[{
          name: 'AGE_AT_EVENT',
          operands:['60', '30'],
          operator:'GREATER_THAN_OR_EQUAL_TO'
        }],
        searchParameters: [{
          name: 'Hypotensive (Systolic <= 90 / Diastolic <= 60)',
          type: 'PM'
        }]
      }]
    }],
    excludes: []
  };
  const activatedRouteStub = {
    data: Observable.of({
      participant: {},
      annotations: [],
    }),
    snapshot: {
      data: {
        workspace: {
          cdrVersionId: 1
        },
        cohort: {
          name: '',
          criteria: JSON.stringify(criteria)
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
    parent: {
      snapshot: {
        data: {
          workspace: {
            cdrVersionId: 1
          },
          cohort: {
            name: ''
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

    },
  };
  let route;

  beforeEach(async(() => {
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
        RouterTestingModule
      ],
      providers: [
        {provide: ActivatedRoute, useValue: activatedRouteStub},
        {provide: CohortBuilderService, useValue: new CohortBuilderServiceStub()},
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
        {provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(QueryReportComponent);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
