import { NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import { ChartModule } from 'angular2-highcharts';
 import {CohortReviewService} from 'generated';
import { HighchartsStatic } from 'angular2-highcharts/dist/HighchartsService';
// import * as highCharts from 'Highcharts';
import {fromJS} from 'immutable';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {CohortSearchActionStub} from 'testing/stubs/cohort-search-action-stub';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';
import {CohortSearchActions} from '../../cohort-search/redux';
import {AnnotationItemComponent} from '../annotation-item/annotation-item.component';
import {AnnotationListComponent} from '../annotation-list/annotation-list.component';
import {CreateReviewPage} from '../create-review-page/create-review-page';
import {DetailAllEventsComponent} from '../detail-all-events/detail-all-events.component';
import {DetailHeaderComponent} from '../detail-header/detail-header.component';
import {DetailTabTableComponent} from '../detail-tab-table/detail-tab-table.component';
import {DetailTabsComponent} from '../detail-tabs/detail-tabs.component';
import {IndividualParticipantsChartsComponent} from '../individual-participants-charts/individual-participants-charts';
import {ParticipantStatusComponent} from '../participant-status/participant-status.component';
import {ReviewStateService} from '../review-state.service';
import {SidebarContentComponent} from '../sidebar-content/sidebar-content.component';
import {DetailPage} from './detail-page';
import * as highCharts from "highcharts";


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
  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        AnnotationItemComponent,
        AnnotationListComponent,
        CreateReviewPage,
        DetailAllEventsComponent,
        DetailHeaderComponent,
        DetailPage,
        DetailTabsComponent,
        IndividualParticipantsChartsComponent,
        DetailTabTableComponent,
        ParticipantStatusComponent,
        SidebarContentComponent,
      ],
      imports: [ClarityModule,
                NgxPopperModule,
                ReactiveFormsModule,
                ChartModule,
                RouterTestingModule],
      providers: [
        {
          provide: HighchartsStatic,
          useValue: highCharts
        },
        {provide: ActivatedRoute, useValue: activatedRouteStub},
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
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
