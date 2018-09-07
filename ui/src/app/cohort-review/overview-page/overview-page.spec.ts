import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {CohortBuilderService} from 'generated';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';

import {ComboChartComponent} from '../../cohort-common/combo-chart/combo-chart.component';
import {ReviewNavComponent} from '../review-nav/review-nav.component';
import {ReviewStateService} from '../review-state.service';
import {OverviewPage} from './overview-page';

describe('OverviewPage', () => {
  let component: OverviewPage;
  let fixture: ComponentFixture<OverviewPage>;
  const activatedRouteStub = {
    parent: {
      snapshot: {
        data: {
          workspace: {
            cdrVersionId: 1
          }
        }
      }
    }
  };
  let route;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ ComboChartComponent, OverviewPage, ReviewNavComponent ],
      imports: [ClarityModule, NgxChartsModule],
      providers: [
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
