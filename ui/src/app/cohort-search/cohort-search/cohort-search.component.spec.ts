import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {ComboChartComponent} from 'app/cohort-common/combo-chart/combo-chart.component';
import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {GenderChartComponent} from 'app/cohort-search/gender-chart/gender-chart.component';
import {ListAttributesPageComponent} from 'app/cohort-search/list-attributes-page/list-attributes-page.component';
import {ListDemographicsComponent} from 'app/cohort-search/list-demographics/list-demographics.component';
import {ListModalComponent} from 'app/cohort-search/list-modal/list-modal.component';
import {ListModifierPageComponent} from 'app/cohort-search/list-modifier-page/list-modifier-page.component';
import {ListNodeInfoComponent} from 'app/cohort-search/list-node-info/list-node-info.component';
import {ListNodeComponent} from 'app/cohort-search/list-node/list-node.component';
import {ListOptionInfoComponent} from 'app/cohort-search/list-option-info/list-option-info.component';
import {ListOverviewComponent} from 'app/cohort-search/list-overview/list-overview.component';
import {ListSearchBarComponent} from 'app/cohort-search/list-search-bar/list-search-bar.component';
import {ListSearchGroupItemComponent} from 'app/cohort-search/list-search-group-item/list-search-group-item.component';
import {ListSearchGroupListComponent} from 'app/cohort-search/list-search-group-list/list-search-group-list.component';
import {ListSearchGroupComponent} from 'app/cohort-search/list-search-group/list-search-group.component';
import {ListSearchComponent} from 'app/cohort-search/list-search/list-search.component';
import {ListSelectionInfoComponent} from 'app/cohort-search/list-selection-info/list-selection-info.component';
import {ListTreeComponent} from 'app/cohort-search/list-tree/list-tree.component';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {SearchGroupSelectComponent} from 'app/cohort-search/search-group-select/search-group-select.component';
import {ConfirmDeleteModalComponent} from 'app/components/confirm-delete-modal';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, queryParamsStore} from 'app/utils/navigation';
import {CohortBuilderService, CohortsService} from 'generated';
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
        CohortSearchComponent,
        ComboChartComponent,
        ConfirmDeleteModalComponent,
        GenderChartComponent,
        ListAttributesPageComponent,
        ListDemographicsComponent,
        ListModalComponent,
        ListModifierPageComponent,
        ListNodeInfoComponent,
        ListNodeComponent,
        ListOptionInfoComponent,
        ListOverviewComponent,
        ListSearchBarComponent,
        ListSearchGroupItemComponent,
        ListSearchGroupListComponent,
        ListSearchGroupComponent,
        ListSearchComponent,
        ListSelectionInfoComponent,
        ListTreeComponent,
        SafeHtmlPipe,
        SearchGroupSelectComponent,
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
