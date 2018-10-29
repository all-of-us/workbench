import {NgRedux} from '@angular-redux/store';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {CohortBuilderService, CohortReviewService} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';
import {CohortReviewServiceStub} from '../../../testing/stubs/cohort-review-service-stub';
import {ComboChartComponent} from '../../cohort-common/combo-chart/combo-chart.component';
import {ParticipantsChartsComponent} from '../participants-charts/participant-charts';
import {ReviewStateService} from '../review-state.service';
import {OverviewPage} from './overview-page';

import {
    CohortSearchActions,
    isChartLoading,
} from '../../cohort-search/redux';



describe('OverviewPage', () => {
  let component: OverviewPage;
  let fixture: ComponentFixture<OverviewPage>;
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
      declarations: [ ComboChartComponent, OverviewPage, ParticipantsChartsComponent],
      imports: [ClarityModule, NgxChartsModule, NgxPopperModule],
      providers: [
        {provide: NgRedux},
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
        {provide: CohortSearchActions},
        {provide: isChartLoading},
        {provide: CohortBuilderService, useValue: new CohortBuilderServiceStub()},
        {provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
        {provide: ActivatedRoute, useValue: activatedRouteStub},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(OverviewPage);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
