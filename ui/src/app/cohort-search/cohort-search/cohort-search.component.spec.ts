import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {ComboChartComponent} from 'app/cohort-common/combo-chart/combo-chart.component';
import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {AttributesPageComponent} from 'app/cohort-search/attributes-page/attributes-page.component';
import {DemographicsComponent} from 'app/cohort-search/demographics/demographics.component';
import {GenderChartComponent} from 'app/cohort-search/gender-chart/gender-chart.component';
import {ListSearchComponent} from 'app/cohort-search/list-search/list-search.component';
import {ModalComponent} from 'app/cohort-search/modal/modal.component';
import {ModifierPageComponent} from 'app/cohort-search/modifier-page/modifier-page.component';
import {NodeInfoComponent} from 'app/cohort-search/node-info/node-info.component';
import {NodeComponent} from 'app/cohort-search/tree-node/tree-node.component';
import {OptionInfoComponent} from 'app/cohort-search/option-info/option-info.component';
import {OverviewComponent} from 'app/cohort-search/overview/overview.component';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {SearchBarComponent} from 'app/cohort-search/search-bar/search-bar.component';
import {SearchGroupListComponent} from 'app/cohort-search/search-group-list/search-group-list.component';
import {wizardStore} from 'app/cohort-search/search-state.service';
import {SelectionInfoComponent} from 'app/cohort-search/selection-info/selection-info.component';
import {TreeComponent} from 'app/cohort-search/tree/tree.component';
import {ConfirmDeleteModalComponent} from 'app/components/confirm-delete-modal';
import {HelpSidebarComponent} from 'app/components/help-sidebar';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, queryParamsStore} from 'app/utils/navigation';
import {CohortBuilderService, CohortsService, DomainType} from 'generated';
import {CohortBuilderApi, CohortsApi} from 'generated/fetch';
import {NouisliderModule} from 'ng2-nouislider';
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
        AttributesPageComponent,
        CohortSearchComponent,
        ComboChartComponent,
        ConfirmDeleteModalComponent,
        GenderChartComponent,
        DemographicsComponent,
        HelpSidebarComponent,
        ModalComponent,
        ModifierPageComponent,
        NodeInfoComponent,
        NodeComponent,
        OptionInfoComponent,
        OverviewComponent,
        SearchBarComponent,
        SearchGroupListComponent,
        ListSearchComponent,
        SelectionInfoComponent,
        TreeComponent,
        SafeHtmlPipe,
        ValidatorErrorsComponent,
      ],
      imports: [
        ClarityModule,
        FormsModule,
        NgxPopperModule,
        NouisliderModule,
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
    wizardStore.next({
      domain: DomainType.MEASUREMENT,
      item: {modifiers: [], searchParameters: []}
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
