import {NgRedux} from '@angular-redux/store';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {ComboChartComponent} from 'app/cohort-common/combo-chart/combo-chart.component';
import {ParticipantsChartsComponent} from 'app/cohort-review/participants-charts/participant-charts';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentCohortStore, currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceAccessLevel} from 'generated';
import {CohortBuilderApi, CohortReviewApi} from 'generated/fetch';
import {NgxPopperModule} from 'ngx-popper';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {CohortReviewServiceStub, cohortReviewStub} from 'testing/stubs/cohort-review-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {OverviewPage} from './overview-page';



describe('OverviewPage', () => {
  let component: OverviewPage;
  let fixture: ComponentFixture<OverviewPage>;

  beforeEach(async(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    TestBed.configureTestingModule({
      declarations: [ ComboChartComponent, OverviewPage, ParticipantsChartsComponent],
      imports: [ClarityModule, NgxChartsModule, NgxPopperModule],
      providers: [
        {provide: NgRedux},
      ],
    })
      .compileComponents();
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      cdrVersionId: '1',
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
    currentCohortStore.next({
      name: '',
      criteria: '{}',
      type: '',
    });
    urlParamsStore.next({
      ns: 'workspaceNamespace',
      wsid: 'workspaceId',
      cid: 1
    });
    cohortReviewStore.next(cohortReviewStub);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(OverviewPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
