import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {ComboChartComponent} from 'app/cohort-common/combo-chart/combo-chart.component';
import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {AttributesPageComponent} from 'app/cohort-search/attributes-page/attributes-page.component';
import {CodeDropdownComponent} from 'app/cohort-search/code-dropdown/code-dropdown.component';
import {DemographicsComponent} from 'app/cohort-search/demographics/demographics.component';
import {GenderChartComponent} from 'app/cohort-search/gender-chart/gender-chart.component';
import {ModalComponent} from 'app/cohort-search/modal/modal.component';
import {ModifierPageComponent} from 'app/cohort-search/modifier-page/modifier-page.component';
import {MultiSelectComponent} from 'app/cohort-search/multi-select/multi-select.component';
import {NodeInfoComponent} from 'app/cohort-search/node-info/node-info.component';
import {NodeComponent} from 'app/cohort-search/node/node.component';
import {OptionInfoComponent} from 'app/cohort-search/option-info/option-info.component';
import {OverviewComponent} from 'app/cohort-search/overview/overview.component';
import {
  cancelWizard,
  clearStore,
  CohortSearchActions,
  finishWizard,
  resetStore
} from 'app/cohort-search/redux';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {SearchBarComponent} from 'app/cohort-search/search-bar/search-bar.component';
import {SearchGroupItemComponent} from 'app/cohort-search/search-group-item/search-group-item.component';
import {SearchGroupListComponent} from 'app/cohort-search/search-group-list/search-group-list.component';
import {SearchGroupSelectComponent} from 'app/cohort-search/search-group-select/search-group-select.component';
import {SearchGroupComponent} from 'app/cohort-search/search-group/search-group.component';
import {SelectionInfoComponent} from 'app/cohort-search/selection-info/selection-info.component';
import {TreeComponent} from 'app/cohort-search/tree/tree.component';
import {currentWorkspaceStore, queryParamsStore} from 'app/utils/navigation';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {CohortBuilderService, CohortsService, WorkspaceAccessLevel} from 'generated';
import {fromJS} from 'immutable';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {CohortSearchComponent} from './cohort-search.component';

class MockActions {
  @dispatch() cancelWizard = cancelWizard;
  @dispatch() finishWizard = finishWizard;
  @dispatch() resetStore = resetStore;
  @dispatch() clearStore = clearStore;
}

describe('CohortSearchComponent', () => {
  let activatedRoute: ActivatedRoute;
  let component: CohortSearchComponent;
  let fixture: ComponentFixture<CohortSearchComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [
        AttributesPageComponent,
        CodeDropdownComponent,
        CohortSearchComponent,
        ComboChartComponent,
        ConfirmDeleteModalComponent,
        DemographicsComponent,
        GenderChartComponent,
        ModalComponent,
        ModifierPageComponent,
        MultiSelectComponent,
        NodeComponent,
        NodeInfoComponent,
        OptionInfoComponent,
        OverviewComponent,
        SafeHtmlPipe,
        SearchBarComponent,
        SearchGroupComponent,
        SearchGroupItemComponent,
        SearchGroupListComponent,
        SearchGroupSelectComponent,
        SelectionInfoComponent,
        TreeComponent,
        ValidatorErrorsComponent,
      ],
      imports: [
        ClarityModule,
        FormsModule,
        NgxChartsModule,
        NgxPopperModule,
        NouisliderModule,
        ReactiveFormsModule
      ],
      providers: [
        {provide: NgRedux, useValue: mockReduxInst},
        {provide: CohortBuilderService, useValue: {}},
        {provide: CohortsService, useValue: {}},
        {provide: CohortSearchActions, useValue: new MockActions()},
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
      ...WorkspacesServiceStub.stubWorkspace(),
      cdrVersionId: '1',
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CohortSearchComponent);
    component = fixture.componentInstance;
    activatedRoute = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
