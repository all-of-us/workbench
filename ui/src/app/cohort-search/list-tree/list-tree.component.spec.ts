import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from '@clr/angular';
import {CodeDropdownComponent} from 'app/cohort-search/code-dropdown/code-dropdown.component';
import {NodeInfoComponent} from 'app/cohort-search/node-info/node-info.component';
import {NodeComponent} from 'app/cohort-search/node/node.component';
import {OptionInfoComponent} from 'app/cohort-search/option-info/option-info.component';
import {
activeCriteriaTreeType,
CohortSearchActions,
criteriaChildren,
criteriaError,
criteriaSearchTerms,
isCriteriaLoading,
} from 'app/cohort-search/redux';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {SearchBarComponent} from 'app/cohort-search/search-bar/search-bar.component';
import {fromJS} from 'immutable';
import {NgxPopperModule} from 'ngx-popper';
import {TreeComponent} from './list-tree.component';

class MockActions {
  @dispatch() activeCriteriaTreeType = activeCriteriaTreeType;
  @dispatch() criteriaChildren = criteriaChildren;
  @dispatch() criteriaError = criteriaError;
  @dispatch() criteriaSearchTerms = criteriaSearchTerms;
  @dispatch() isCriteriaLoading = isCriteriaLoading;

  fetchCriteria(kind: string, parentId: number): void {}
}

describe('TreeComponent', () => {
  let component: TreeComponent;
  let fixture: ComponentFixture<TreeComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [
        CodeDropdownComponent,
        TreeComponent,
        NodeComponent,
        NodeInfoComponent,
        OptionInfoComponent,
        SafeHtmlPipe,
        SearchBarComponent,
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
        {provide: CohortSearchActions, useValue: new MockActions()},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TreeComponent);
    component = fixture.componentInstance;
    component.node = fromJS({
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
    });
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
