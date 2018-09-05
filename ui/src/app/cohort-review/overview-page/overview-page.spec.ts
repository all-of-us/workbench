import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {ChartInfoListResponse, Cohort, CohortBuilderService} from 'generated';
import {Observable} from 'rxjs/Observable';

import {ComboChartComponent} from '../../cohort-common/combo-chart/combo-chart.component';
import {ReviewNavComponent} from '../review-nav/review-nav.component';
import {ReviewStateService} from '../review-state.service';
import {OverviewPage} from './overview-page';

describe('OverviewPage', () => {
  let component: OverviewPage;
  let fixture: ComponentFixture<OverviewPage>;
  let route;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ ComboChartComponent, OverviewPage, ReviewNavComponent ],
      imports: [ClarityModule, NgxChartsModule],
      providers: [
        {provide: CohortBuilderService, useValue: {
          getChartInfo:  (): Observable<ChartInfoListResponse> => {
            return Observable.of(<ChartInfoListResponse> {items: []});
          }
        }},
        {provide: ReviewStateService, useValue: {
          cohort$: Observable.of(<Cohort>{
            name: 'test',
            criteria: '[]',
            type: 'type'
          })
        }},
        {provide: ActivatedRoute, useValue: {
          parent: {snapshot: {data: {workspace: {cdrVersionId: 1}}}}
        }},
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
