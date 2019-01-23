import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {ChartModule} from 'angular2-highcharts';
import { HighchartsStatic } from 'angular2-highcharts/dist/HighchartsService';
import {IndividualParticipantsChartsComponent} from 'app/cohort-review/individual-participants-charts/individual-participants-charts';
import {ReviewStateService} from 'app/cohort-review/review-state.service';
import {CohortReviewService} from 'generated';
import * as highCharts from 'highcharts';
import {Observable} from 'rxjs/Observable';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';
import {DetailTabsComponent} from './detail-tabs.component';



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

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [DetailTabsComponent, IndividualParticipantsChartsComponent],
      imports: [ChartModule, ClarityModule, RouterTestingModule],
       schemas: [NO_ERRORS_SCHEMA],
      providers: [
        {
          provide: HighchartsStatic,
          useValue: highCharts
        },
        {provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
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
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
