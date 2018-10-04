import {dispatch, NgRedux} from '@angular-redux/store';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {AnnotationType, CohortAnnotationDefinition, CohortReviewService} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';
import {AnnotationItemComponent} from '../annotation-item/annotation-item.component';
import {AnnotationListComponent} from '../annotation-list/annotation-list.component';
import {DetailAllEventsComponent} from '../detail-all-events/detail-all-events.component';
import {DetailHeaderComponent} from '../detail-header/detail-header.component';
import {DetailTabTableComponent} from '../detail-tab-table/detail-tab-table.component';
import {DetailTabsComponent} from '../detail-tabs/detail-tabs.component';
import {ParticipantStatusComponent} from '../participant-status/participant-status.component';
import {ReviewStateService} from '../review-state.service';
import {SidebarContentComponent} from '../sidebar-content/sidebar-content.component';
import {DetailPage} from './detail-page';
import {IndividualParticipantsChartsComponent} from '../individual-participants-charts/individual-participants-charts';
import { ChartModule } from 'angular2-highcharts';
import * as highCharts from 'Highcharts';
import {
  addAttributeForPreview, addParameter,
  CohortSearchActions,
  CohortSearchState,
  getParticipantData, hideAttributesPage,
  isParticipantIdExists, requestAttributePreview
} from '../../cohort-search/redux';
import {MockNgRedux} from "@angular-redux/store/testing";
import {fromJS, List, has, getIn} from "immutable";
import {initialState, SR_ID, } from "../../cohort-search/redux/store";
import { RouterTestingModule } from '@angular/router/testing';
import {CohortSearchActionStub} from 'testing/stubs/cohort-search-action-stub';

  // @dispatch() addParameter = addParameter;
  // @dispatch() hideAttributesPage = hideAttributesPage;
  // @dispatch() requestAttributePreview = requestAttributePreview;
describe('DetailPage', () => {
  let component: DetailPage;
  let fixture: ComponentFixture<DetailPage>;

  const routerSpy = jasmine.createSpyObj('Router', ['navigateByUrl']);
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
          }
        },
        params: {
          ns: '',
          wsid: '',
          cid: ''
        }
      }
    },
    params: {
      ns: '',
      wsid: '',
      cid: ''
    }



  };
  let route;
  let mockReduxInst;
  beforeEach(async(() => {
     // const store = initialState;
     mockReduxInst = MockNgRedux.getInstance();
     const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;
    // store.has(activatedRouteStub.parent.snapshot.params.cid);
    // mockReduxInst.getState.getIn(['individualChartData', 'chartsData', SR_ID]).has(activatedRouteStub.parent.snapshot.params.cid)
    TestBed.configureTestingModule({
      declarations: [
        AnnotationItemComponent,
        AnnotationListComponent,
        DetailAllEventsComponent,
        DetailHeaderComponent,
        DetailPage,
        DetailTabsComponent,
        IndividualParticipantsChartsComponent,
        DetailTabTableComponent,
        ParticipantStatusComponent,
        SidebarContentComponent,
      ],
      imports: [ClarityModule, NgxPopperModule, ReactiveFormsModule, ChartModule.forRoot(highCharts), RouterTestingModule],
      providers: [
        {provide: NgRedux, useValue: mockReduxInst},
        {provide: ActivatedRoute, useValue: activatedRouteStub},
        {provide: CohortReviewService, useValue: {}},
        {provide: CohortSearchActions, useValue: new CohortSearchActionStub()},
        {provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
        {provide: Router, useValue: routerSpy},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailPage);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
