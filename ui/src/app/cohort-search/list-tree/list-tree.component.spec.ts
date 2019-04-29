import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from '@clr/angular';
import {CodeDropdownComponent} from 'app/cohort-search/code-dropdown/code-dropdown.component';
import {ListNodeInfoComponent} from 'app/cohort-search/list-node-info/list-node-info.component';
import {ListNodeComponent} from 'app/cohort-search/list-node/list-node.component';
import {ListOptionInfoComponent} from 'app/cohort-search/list-option-info/list-option-info.component';
import {ListSearchBarComponent} from 'app/cohort-search/list-search-bar/list-search-bar.component';
import {
activeCriteriaTreeType,
CohortSearchActions,
criteriaChildren,
criteriaError,
criteriaSearchTerms,
isCriteriaLoading,
} from 'app/cohort-search/redux';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortBuilderService, WorkspaceAccessLevel} from 'generated';
import {fromJS} from 'immutable';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {ListTreeComponent} from './list-tree.component';

class MockActions {
  @dispatch() activeCriteriaTreeType = activeCriteriaTreeType;
  @dispatch() criteriaChildren = criteriaChildren;
  @dispatch() criteriaError = criteriaError;
  @dispatch() criteriaSearchTerms = criteriaSearchTerms;
  @dispatch() isCriteriaLoading = isCriteriaLoading;

  fetchCriteria(kind: string, parentId: number): void {}
}

describe('ListTreeComponent', () => {
  let component: ListTreeComponent;
  let fixture: ComponentFixture<ListTreeComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [
        CodeDropdownComponent,
        ListNodeComponent,
        ListNodeInfoComponent,
        ListOptionInfoComponent,
        ListSearchBarComponent,
        ListTreeComponent,
        SafeHtmlPipe,
      ],
      imports: [
        BrowserAnimationsModule,
        ClarityModule,
        FormsModule,
        NgxPopperModule,
        ReactiveFormsModule,
      ],
      providers: [
        {provide: NgRedux, useValue: mockReduxInst},
        {provide: CohortBuilderService, useValue: {getCriteriaBy: () => {
          return Observable.of({});
        }}},
        {provide: CohortSearchActions, useValue: new MockActions()},
      ],
    })
      .compileComponents();
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      cdrVersionId: '1',
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ListTreeComponent);
    component = fixture.componentInstance;
    component.node = {
      code: '',
      conceptId: 903133,
      count: 0,
      domainId: 'Measurement',
      group: false,
      hasAttributes: true,
      id: 316305,
      name: 'Height Detail',
      parentId: 0,
      predefinedAttributes: null,
      selectable: true,
      subtype: 'HEIGHT',
      type: 'PM'
    };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
