import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import { ChartModule } from 'angular2-highcharts';
import { HighchartsStatic } from 'angular2-highcharts/dist/HighchartsService';
import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {AddAnnotationDefinitionModalComponent, EditAnnotationDefinitionsModalComponent} from 'app/cohort-review/annotation-definition-modals/annotation-definition-modals.component';
import {ClearButtonInMemoryFilterComponent} from 'app/cohort-review/clearbutton-in-memory-filter/clearbutton-in-memory-filter.component';
import {CreateReviewPage} from 'app/cohort-review/create-review-page/create-review-page';
import {DetailHeaderComponent} from 'app/cohort-review/detail-header/detail-header.component';
import {DetailTabTableComponent} from 'app/cohort-review/detail-tab-table/detail-tab-table.component';
import {DetailTabsComponent} from 'app/cohort-review/detail-tabs/detail-tabs.component';
import {IndividualParticipantsChartsComponent} from 'app/cohort-review/individual-participants-charts/individual-participants-charts';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {SidebarContentComponent} from 'app/cohort-review/sidebar-content/sidebar-content.component';
import {CohortSearchActions} from 'app/cohort-search/redux';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortAnnotationDefinitionService, CohortReviewService, WorkspaceAccessLevel} from 'generated';
import * as highCharts from 'highcharts';
import {NgxPopperModule} from 'ngx-popper';
import {CohortAnnotationDefinitionServiceStub} from 'testing/stubs/cohort-annotation-definition-service-stub';
import {CohortReviewServiceStub, cohortReviewStub} from 'testing/stubs/cohort-review-service-stub';
import {CohortSearchActionStub} from 'testing/stubs/cohort-search-action-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {DetailPage} from './detail-page';



describe('DetailPage', () => {
  let component: DetailPage;
  let fixture: ComponentFixture<DetailPage>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        ClearButtonInMemoryFilterComponent,
        CreateReviewPage,
        DetailHeaderComponent,
        DetailPage,
        DetailTabsComponent,
        IndividualParticipantsChartsComponent,
        DetailTabTableComponent,
        AddAnnotationDefinitionModalComponent,
        EditAnnotationDefinitionsModalComponent,
        SidebarContentComponent,
        ValidatorErrorsComponent,
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
        {
          provide: CohortAnnotationDefinitionService,
          useValue: new CohortAnnotationDefinitionServiceStub()
        },
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
        {provide: CohortSearchActions, useValue: new CohortSearchActionStub()},
      ],
    })
      .compileComponents();
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
    cohortReviewStore.next(cohortReviewStub);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
