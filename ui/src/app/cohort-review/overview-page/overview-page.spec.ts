import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {CohortBuilderService, CohortReviewService} from 'generated';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';
import {ComboChartComponent} from '../../cohort-common/combo-chart/combo-chart.component';
import {ReviewNavComponent} from '../review-nav/review-nav.component';
import {ReviewStateService} from '../review-state.service';
import {OverviewPage} from './overview-page';
import {NgxPopperModule} from 'ngx-popper';
import {NgRedux} from '@angular-redux/store';
import {CohortCommonModule} from '../../cohort-common/module.ts';
import {
    CohortSearchActions,
    isChartLoading,
} from '../../cohort-search/redux';

describe('OverviewPage', () => {
  let component: OverviewPage;
  let fixture: ComponentFixture<OverviewPage>;
  const activatedRouteStub = {
    parent: {
      snapshot: {
        data: {
          workspace: {
            cdrVersionId: 1
          },
            cohort: {
                name: ''
            }
        }
      }
    },
  };
  let route;
    // const {cdrVersionId} = this.route.parent.snapshot.data.workspace;
    // this.selectedCohortName = this.route.parent.snapshot.data.cohort.name;
  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ ComboChartComponent, OverviewPage, ReviewNavComponent ],
      imports: [ClarityModule, NgxChartsModule, NgxPopperModule,CohortCommonModule],
      providers: [
        {provide: NgRedux},
        {provide:CohortReviewService},
        {provide:CohortSearchActions},
        {provide:isChartLoading},
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
