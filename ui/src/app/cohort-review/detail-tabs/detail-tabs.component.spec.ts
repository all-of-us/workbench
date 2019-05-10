import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {ChartModule} from 'angular2-highcharts';
import { HighchartsStatic } from 'angular2-highcharts/dist/HighchartsService';
import {IndividualParticipantsChartsComponent} from 'app/cohort-review/individual-participants-charts/individual-participants-charts';
import {filterStateStore} from 'app/cohort-review/review-state.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortReviewApi, WorkspaceAccessLevel} from 'generated/fetch';
import * as highCharts from 'highcharts';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {workspaceStubs} from 'testing/stubs/workspaces-api-stub';
import {DetailTabsComponent} from './detail-tabs.component';


describe('DetailTabsComponent', () => {
  let component: DetailTabsComponent;
  let fixture: ComponentFixture<DetailTabsComponent>;

  beforeEach(async(() => {
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    TestBed.configureTestingModule({
      declarations: [DetailTabsComponent, IndividualParticipantsChartsComponent],
      imports: [ChartModule, ClarityModule, RouterTestingModule],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        {
          provide: HighchartsStatic,
          useValue: highCharts
        },
      ]
    })
      .compileComponents();
    currentWorkspaceStore.next({
      ...workspaceStubs[0],
      cdrVersionId: '1',
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
    filterStateStore.next({vocab: 'standard'});
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailTabsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
