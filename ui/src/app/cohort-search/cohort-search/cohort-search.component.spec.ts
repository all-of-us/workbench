import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {ComboChartComponent} from 'app/cohort-common/combo-chart/combo-chart.component';
import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {ModalComponent} from 'app/cohort-search/modal/modal.component';
import {OverviewComponent} from 'app/cohort-search/overview/overview.component';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {SearchGroupListComponent} from 'app/cohort-search/search-group-list/search-group-list.component';
import {ConfirmDeleteModalComponent} from 'app/components/confirm-delete-modal';
import {HelpSidebarComponent} from 'app/components/help-sidebar';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, queryParamsStore} from 'app/utils/navigation';
import {CohortBuilderService, CohortsService} from 'generated';
import {CohortBuilderApi, CohortsApi} from 'generated/fetch';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {CohortsApiStub} from 'testing/stubs/cohorts-api-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {CohortSearchComponent} from './cohort-search.component';

describe('CohortSearchComponent', () => {
  let activatedRoute: ActivatedRoute;
  let component: CohortSearchComponent;
  let fixture: ComponentFixture<CohortSearchComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        CohortSearchComponent,
        ComboChartComponent,
        ConfirmDeleteModalComponent,
        HelpSidebarComponent,
        ModalComponent,
        OverviewComponent,
        SearchGroupListComponent,
        SafeHtmlPipe,
        ValidatorErrorsComponent,
      ],
      imports: [
        ClarityModule,
        FormsModule,
        NgxPopperModule,
        ReactiveFormsModule
      ],
      providers: [
        {provide: CohortBuilderService, useValue: {}},
        {provide: CohortsService, useValue: {}},
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: Observable.of({criteria: '{"excludes":[],"includes":[]}'}),
            data: Observable.of({workspace: {cdrVersionId: '1'}})
          }
        },
      ],
    })
      .compileComponents();
    queryParamsStore.next({
      criteria: '{"excludes":[],"includes":[]}'
    });
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
  }));

  beforeEach(() => {
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    fixture = TestBed.createComponent(CohortSearchComponent);
    component = fixture.componentInstance;
    activatedRoute = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
