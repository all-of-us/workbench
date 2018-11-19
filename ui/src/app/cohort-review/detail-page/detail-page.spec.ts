import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import { ChartModule } from 'angular2-highcharts';
import { HighchartsStatic } from 'angular2-highcharts/dist/HighchartsService';
import {CohortAnnotationDefinitionService, CohortReviewService} from 'generated';
import * as highCharts from 'highcharts';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {ClearButtonFilterComponent} from '../clearbutton-in-memory-filter';
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
import {SetAnnotationCreateComponent} from '../set-annotation-create/set-annotation-create.component';
import {SetAnnotationItemComponent} from '../set-annotation-item/set-annotation-item.component';
import {SetAnnotationListComponent} from '../set-annotation-list/set-annotation-list.component';
import {SetAnnotationModalComponent} from '../set-annotation-modal/set-annotation-modal.component';
import {SidebarContentComponent} from '../sidebar-content/sidebar-content.component';
import {DetailPage} from './detail-page';



describe('DetailPage', () => {
  let component: DetailPage;
  let fixture: ComponentFixture<DetailPage>;

  const routerSpy = jasmine.createSpyObj('Router', ['navigateByUrl']);
  const activatedRouteStub = {
    data: Observable.of({
      participant: {},
      annotations: [],
    }),
    snapshot: {
      data: {},
    },
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
        SetAnnotationCreateComponent,
        SetAnnotationItemComponent,
        SetAnnotationListComponent,
        SetAnnotationModalComponent,
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
        {provide: CohortAnnotationDefinitionService, useValue: {}},
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
