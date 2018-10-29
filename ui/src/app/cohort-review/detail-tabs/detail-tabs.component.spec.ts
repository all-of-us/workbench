import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ChartModule} from 'angular2-highcharts';
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
import {DetailTabsComponent} from './detail-tabs.component';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {CohortStatus} from "../../../generated";
// import {CohortReviewService} from "../../../generated";
class ApiSpy {
  getParticipantChartData = jasmine
    .createSpy('getParticipantChartData')
    .and
    .returnValue(Observable.of({

      ageAtEvent: 16,
      rank: 1,
      standardName: "Sprain of cruciate ligament of knee",
      standardVocabulary: "SNOMED",
      startDate: "2006-07-03"


    }));
}
describe('DetailTabsComponent', () => {
  let component: DetailTabsComponent;
  let fixture: ComponentFixture<DetailTabsComponent>;
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
  let cohortReviewService: CohortReviewService;
  // let mockReduxInst;

  beforeEach(async(() => {
     // const store = initialState;
   //  mockReduxInst = MockNgRedux.getInstance();
   //   const _old = mockReduxInst.getState;
   //   const _wrapped = () => fromJS(_old());
   //   mockReduxInst.getState = _wrapped;
   // store.has(activatedRouteStub.parent.snapshot.params.cid);
    TestBed.configureTestingModule({
      declarations: [DetailTabsComponent, IndividualParticipantsChartsComponent],
      imports: [ChartModule, RouterTestingModule],
       schemas: [NO_ERRORS_SCHEMA],
      providers: [
        // {provide: NgRedux, useValue: mockReduxInst},
        {provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
        // {provide: CohortSearchActions, useValue: new CohortSearchActionStub()},
        {provide: ActivatedRoute, useValue: activatedRouteStub},
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailTabsComponent);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
    // cohortReviewService = TestBed.get(CohortReviewService);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
