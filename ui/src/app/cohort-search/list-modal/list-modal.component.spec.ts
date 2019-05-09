import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {CodeDropdownComponent} from 'app/cohort-search/code-dropdown/code-dropdown.component';
import {DemographicsComponent} from 'app/cohort-search/demographics/demographics.component';
import {ListAttributesPageComponent} from 'app/cohort-search/list-attributes-page/list-attributes-page.component';
import {ListModifierPageComponent} from 'app/cohort-search/list-modifier-page/list-modifier-page.component';
import {ListNodeInfoComponent} from 'app/cohort-search/list-node-info/list-node-info.component';
import {ListNodeComponent} from 'app/cohort-search/list-node/list-node.component';
import {ListOptionInfoComponent} from 'app/cohort-search/list-option-info/list-option-info.component';
import {ListSearchBarComponent} from 'app/cohort-search/list-search-bar/list-search-bar.component';
import {ListSearchComponent} from 'app/cohort-search/list-search/list-search.component';
import {ListSelectionInfoComponent} from 'app/cohort-search/list-selection-info/list-selection-info.component';
import {ListTreeComponent} from 'app/cohort-search/list-tree/list-tree.component';
import {MultiSelectComponent} from 'app/cohort-search/multi-select/multi-select.component';
import {NodeInfoComponent} from 'app/cohort-search/node-info/node-info.component';
import {NodeComponent} from 'app/cohort-search/node/node.component';
import {OptionInfoComponent} from 'app/cohort-search/option-info/option-info.component';
import {
activeCriteriaTreeType,
activeCriteriaType,
activeParameterList,
CohortSearchActions,
nodeAttributes,
wizardOpen
} from 'app/cohort-search/redux';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {SearchBarComponent} from 'app/cohort-search/search-bar/search-bar.component';
import {wizardStore} from 'app/cohort-search/search-state.service';
import {TreeComponent} from 'app/cohort-search/tree/tree.component';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {DomainType, WorkspaceAccessLevel} from 'generated';
import {CohortBuilderApi} from 'generated/fetch';
import {fromJS, Map} from 'immutable';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {ListModalComponent} from './list-modal.component';

class MockActions {
  @dispatch() activeCriteriaType = activeCriteriaType;
  @dispatch() activeCriteriaTreeType = activeCriteriaTreeType;
  @dispatch() activeParameterList = activeParameterList;
  @dispatch() attributesPage = nodeAttributes;
  @dispatch() wizardOpen = wizardOpen;
}

describe('ListModalComponent', () => {
  let component: ListModalComponent;
  let fixture: ComponentFixture<ListModalComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [
        CodeDropdownComponent,
        DemographicsComponent,
        ListAttributesPageComponent,
        ListModalComponent,
        ListModifierPageComponent,
        ListNodeInfoComponent,
        ListNodeComponent,
        ListOptionInfoComponent,
        ListSearchBarComponent,
        ListSearchComponent,
        ListSelectionInfoComponent,
        ListTreeComponent,
        MultiSelectComponent,
        NodeComponent,
        NodeInfoComponent,
        OptionInfoComponent,
        SafeHtmlPipe,
        SearchBarComponent,
        TreeComponent,
        ValidatorErrorsComponent,
      ],
      imports: [
        ClarityModule,
        FormsModule,
        NgxPopperModule,
        NouisliderModule,
        ReactiveFormsModule,
      ],
      providers: [
        {provide: NgRedux, useValue: mockReduxInst},
        {provide: CohortSearchActions, useValue: new MockActions()},
      ],
    })
      .compileComponents();
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
    wizardStore.next({
      domain: DomainType.MEASUREMENT,
      item: {modifiers: [], searchParameters: []}
    });
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ListModalComponent);
    component = fixture.componentInstance;
    component.attributesNode = Map();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
