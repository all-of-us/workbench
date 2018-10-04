import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ChartModule} from 'angular2-highcharts';
import { HighchartsStatic } from 'angular2-highcharts/dist/HighchartsService';
import {CohortReviewService} from 'generated';
// import * as highCharts from 'Highcharts';
import {fromJS} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {CohortSearchActionStub} from 'testing/stubs/cohort-search-action-stub';
import {ReviewStateServiceStub} from '../../../testing/stubs/review-state-service-stub';
import {CohortSearchActions} from '../../cohort-search/redux';
import {initialState} from '../../cohort-search/redux/store';
import {IndividualParticipantsChartsComponent} from '../individual-participants-charts/individual-participants-charts';
import {ReviewStateService} from '../review-state.service';
// import {highchartsFactory} from "../cohort-review.module";
import * as highCharts from 'highcharts';


describe('IndividualParticipantsChartsComponent', () => {
  let component: IndividualParticipantsChartsComponent;
  let fixture: ComponentFixture<IndividualParticipantsChartsComponent>;
  const activatedRouteStub = {
    data: Observable.of({
      participant: {},
      annotations: [],
    }),
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
            ns: '',
            wsid: '',
            cid: ''
          }
        },
        params: {
          ns: '',
          wsid: '',
          cid: ''
        }
      }
    },

  };
  let route;
  let mockReduxInst;

  beforeEach(async(() => {
    const store = initialState;
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;
    store.has(activatedRouteStub.parent.snapshot.params.cid);
    TestBed.configureTestingModule({
      declarations: [IndividualParticipantsChartsComponent],
      imports: [ChartModule, RouterTestingModule],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        {
          provide: HighchartsStatic,
          useValue: highCharts
        },
        {provide: NgRedux, useValue: mockReduxInst},
        {provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
        {provide: CohortReviewService, useValue: {}},
        {provide: CohortSearchActions, useValue: new CohortSearchActionStub()},
        {provide: ActivatedRoute, useValue: activatedRouteStub},
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(IndividualParticipantsChartsComponent);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
