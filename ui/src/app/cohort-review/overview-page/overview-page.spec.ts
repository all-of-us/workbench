import {NgRedux} from '@angular-redux/store';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {ComboChartComponent} from 'app/cohort-common/combo-chart/combo-chart.component';
import {ParticipantsChartsComponent} from 'app/cohort-review/participants-charts/participant-charts';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {currentCohortStore, currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {CohortBuilderService, CohortReviewService, WorkspaceAccessLevel} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {CohortReviewServiceStub, cohortReviewStub} from 'testing/stubs/cohort-review-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {OverviewPage} from './overview-page';



describe('OverviewPage', () => {
  let component: OverviewPage;
  let fixture: ComponentFixture<OverviewPage>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ ComboChartComponent, OverviewPage, ParticipantsChartsComponent],
      imports: [ClarityModule, NgxChartsModule, NgxPopperModule],
      providers: [
        {provide: NgRedux},
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
        {provide: CohortBuilderService, useValue: new CohortBuilderServiceStub()},
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
