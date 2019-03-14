import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {ChartModule} from 'angular2-highcharts';
import { HighchartsStatic } from 'angular2-highcharts/dist/HighchartsService';
import {IndividualParticipantsChartsComponent} from 'app/cohort-review/individual-participants-charts/individual-participants-charts';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspaceAccessLevel} from 'generated';
import {CohortReviewApi} from 'generated/fetch';
import * as highCharts from 'highcharts';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
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
      ...WorkspacesServiceStub.stubWorkspace(),
      cdrVersionId: '1',
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
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
