import {async as _async, ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {ClarityModule} from 'clarity-angular';
import {MockNgRedux} from '@angular-redux/store/testing';
import {Map, List, fromJS} from 'immutable';
import {NgRedux, dispatch} from '@angular-redux/store';

import {
  activeCriteriaType,
  activeParameterList,
  CohortSearchActions,
  CohortSearchState,
  REMOVE_PARAMETER,
  removeParameter,
} from '../../redux';
import {SelectionComponent} from './selection.component';
import {CohortBuilderService} from 'generated';

const TYPE_ICD9 = 'icd9';
const TYPE_DEMO = 'demo';

const SELECTION_ICD9 = fromJS([
  {
    type: 'icd9',
    name: 'CodeA',
    id: 'CodeA',
    parameterId: 'CodeA',
  }, {
    type: 'icd9',
    name: 'CodeB',
    id: 'CodeB',
    parameterId: 'CodeB',
  }
]);

const SELECTION_DEMO = fromJS([
  {
    type: 'DEMO',
    subtype: 'GEN',
    name: 'Female',
    code: 'F',
    id: 0,
    parameterId: 'Code0',
  }, {
    type: 'DEMO',
    subtype: 'RACE',
    name: 'African American',
    code: 'A',
    id: 1,
    parameterId: 'Code1',
  }, {
    type: 'DEMO',
    subtype: 'AGE',
    id: 2,
    parameterId: 'Code0',
  }, {
    type: 'DEMO',
    subtype: 'DEC',
    id: 3,
    parameterId: 'Code0',
  }
]);

class MockActions {
  @dispatch() removeParameter = removeParameter;
}

describe('SelectionComponent', () => {
  let fixture: ComponentFixture<SelectionComponent>;
  let comp: SelectionComponent;
  let mockReduxInst;

  let dispatchSpy;
  let typeStub;
  let listStub;

  beforeEach(_async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed
      .configureTestingModule({
        declarations: [SelectionComponent],
        imports: [ClarityModule],
        providers: [
          {provide: NgRedux, useValue: mockReduxInst},
          {provide: CohortBuilderService, useValue: {}},
          {provide: CohortSearchActions, useValue: new MockActions()},
        ],
      })
      .compileComponents();
  }));

  beforeEach(() => {
    MockNgRedux.reset();
    dispatchSpy = spyOn(mockReduxInst, 'dispatch');
    fixture = TestBed.createComponent(SelectionComponent);
    comp = fixture.componentInstance;

    typeStub = MockNgRedux
      .getSelectorStub<CohortSearchState, string>(
        activeCriteriaType);

    listStub = MockNgRedux
      .getSelectorStub<CohortSearchState, List<any>>(
        activeParameterList);

    fixture.detectChanges();
  });

  it('Should render', () => {
    expect(comp).toBeTruthy();
  });

  it('Should generate the correct title', () => {
    const title = fixture.debugElement.query(By.css('h5'));

    typeStub.next(TYPE_ICD9);
    fixture.detectChanges();
    expect(title.nativeElement.textContent).toBe('Selected ICD9 Codes');

    typeStub.next(TYPE_DEMO);
    fixture.detectChanges();
    expect(title.nativeElement.textContent).toBe('Selected Demographics Codes');

    typeStub.next(null);
    fixture.detectChanges();
    expect(title.nativeElement.textContent).toBe('No Selection');
  });

  it('should dispatch REMOVE_PARAMETER on removal click', () => {
    typeStub.next(TYPE_ICD9);
    listStub.next(SELECTION_ICD9);
    fixture.detectChanges();

    const selector = 'div#wizard-parameter-container button.text-danger';
    const button = fixture.debugElement.query(By.css(selector));
    button.triggerEventHandler('click', null);

    expect(dispatchSpy).toHaveBeenCalledWith({
      type: REMOVE_PARAMETER,
      parameterId: 'CodeA',
    });
  });

});
