import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from '@clr/angular';
import {NodeInfoComponent} from 'app/cohort-search/node-info/node-info.component';
import {
  activeCriteriaTreeType,
  CohortSearchActions,
  criteriaChildren,
  criteriaError,
  criteriaSearchTerms,
  isCriteriaLoading,
} from 'app/cohort-search/redux';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {fromJS} from 'immutable';
import {NgxPopperModule} from 'ngx-popper';
import {NodeComponent} from './list-node.component';

class MockActions {
  @dispatch() activeCriteriaTreeType = activeCriteriaTreeType;
  @dispatch() criteriaChildren = criteriaChildren;
  @dispatch() criteriaError = criteriaError;
  @dispatch() criteriaSearchTerms = criteriaSearchTerms;
  @dispatch() isCriteriaLoading = isCriteriaLoading;
}

describe('NodeComponent', () => {
  let component: NodeComponent;
  let fixture: ComponentFixture<NodeComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [
        NodeComponent,
        NodeInfoComponent,
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
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NodeComponent);
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
