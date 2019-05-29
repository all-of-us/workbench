import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from '@clr/angular';
import {ListNodeInfoComponent} from 'app/cohort-search/list-node-info/list-node-info.component';
import {
  activeCriteriaTreeType,
  CohortSearchActions,
  criteriaChildren,
  criteriaError,
  criteriaSearchTerms,
  isCriteriaLoading,
} from 'app/cohort-search/redux';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortBuilderApi} from 'generated/fetch';
import {fromJS} from 'immutable';
import {NgxPopperModule} from 'ngx-popper';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {ListNodeComponent} from './list-node.component';

class MockActions {
  @dispatch() activeCriteriaTreeType = activeCriteriaTreeType;
  @dispatch() criteriaChildren = criteriaChildren;
  @dispatch() criteriaError = criteriaError;
  @dispatch() criteriaSearchTerms = criteriaSearchTerms;
  @dispatch() isCriteriaLoading = isCriteriaLoading;
}

describe('ListNodeComponent', () => {
  let component: ListNodeComponent;
  let fixture: ComponentFixture<ListNodeComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [
        ListNodeInfoComponent,
        ListNodeComponent,
        SafeHtmlPipe,
      ],
      imports: [
        BrowserAnimationsModule,
        ClarityModule,
        NgxPopperModule,
        ReactiveFormsModule,
      ],
      providers: [
        {provide: NgRedux, useValue: mockReduxInst},
        {provide: CohortSearchActions, useValue: new MockActions()},
      ],
    })
      .compileComponents();
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
  }));

  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    fixture = TestBed.createComponent(ListNodeComponent);
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
