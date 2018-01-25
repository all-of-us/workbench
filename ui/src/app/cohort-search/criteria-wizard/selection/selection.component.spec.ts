import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {ClarityModule} from 'clarity-angular';
import {fromJS} from 'immutable';

import {
  activeCriteriaType,
  activeParameterList,
  CohortSearchActions,
  /* tslint:disable-next-line:no-unused-variable */
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

  beforeEach(async(() => {
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
      .getSelectorStub<CohortSearchState, any>(
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
