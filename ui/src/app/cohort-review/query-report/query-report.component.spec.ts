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

import { QueryReportComponent } from './query-report.component';

describe('QueryReportComponent', () => {
  let component: QueryReportComponent;
  let fixture: ComponentFixture<QueryReportComponent>;
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
          criteria: '{"includes":[{"id":"includes_k6chli1bh","items":[{"id":"items_c0xgoxmfq","type":"PM","searchParameters":[{"parameterId":"param3272848","name":"Hypotensive (Systolic <= 90 / Diastolic <= 60)","type":"PM","subtype":"BP","group":false,"attributes":[{"conceptId":903118,"name":"Systolic","operands":["90"],"operator":"LESS_THAN_OR_EQUAL_TO"},{"conceptId":903115,"name":"Diastolic","operands":["60"],"operator":"LESS_THAN_OR_EQUAL_TO"}],"domainId":"Measurement"}],"modifiers":[]}],"temporal":false}],"excludes":[]}'
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
        QueryReportComponent
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
