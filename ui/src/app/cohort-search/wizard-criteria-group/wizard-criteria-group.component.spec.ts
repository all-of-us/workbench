import {async as _async, ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {ClarityModule} from 'clarity-angular';
import {MockNgRedux} from '@angular-redux/store/testing';
import {Map, List, fromJS} from 'immutable';
import {NgRedux} from '@angular-redux/store';

import {
  activeCriteriaType,
  activeCriteriaList,
  CohortSearchActions,
  CohortSearchState,
  UNSELECT_CRITERIA,
} from '../redux';
import {WizardCriteriaGroupComponent} from './wizard-criteria-group.component';
import {CohortBuilderService} from 'generated';

const TYPE_ICD9 = 'icd9';
const TYPE_DEMO = 'demo';

const SELECTION_ICD9 = fromJS([
  {
    type: 'icd9',
    name: 'CodeA',
    id: 'CodeA',
  }, {
    type: 'icd9',
    name: 'CodeB',
    id: 'CodeB',
  }
]);

const SELECTION_DEMO = fromJS([
  {
    type: 'DEMO_GEN',
    name: 'Female',
    code: 'F',
    id: 0,
  }, {
    type: 'DEMO_RACE',
    name: 'African American',
    code: 'A',
    id: 1,
  }, {
    type: 'DEMO_AGE',
    id: 2,
  }, {
    type: 'DEMO_DEC',
    id: 3,
  }
]);

describe('WizardCriteriaGroupComponent', () => {
  let fixture: ComponentFixture<WizardCriteriaGroupComponent>;
  let comp: WizardCriteriaGroupComponent;

  let dispatchSpy;
  let mockReduxInst;
  let typeStub;
  let listStub;

  beforeEach(_async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed
      .configureTestingModule({
        declarations: [WizardCriteriaGroupComponent],
        imports: [ClarityModule],
        providers: [
          {provide: NgRedux, useValue: mockReduxInst},
          {provide: CohortBuilderService, useValue: {}},
          CohortSearchActions,
        ],
      })
      .compileComponents();
  }));

  beforeEach(() => {
    MockNgRedux.reset();
    dispatchSpy = spyOn(mockReduxInst, 'dispatch');
    fixture = TestBed.createComponent(WizardCriteriaGroupComponent);
    comp = fixture.componentInstance;

    typeStub = MockNgRedux
      .getSelectorStub<CohortSearchState, string>(
        activeCriteriaType);

    listStub = MockNgRedux
      .getSelectorStub<CohortSearchState, List<any>>(
        activeCriteriaList);

    fixture.detectChanges();
  });

  it('Should render', () => {
    typeStub.next(TYPE_ICD9);
    listStub.next(SELECTION_ICD9);
    expect(comp).toBeTruthy();
    fixture.detectChanges();

    const selector = 'div#wizard-criteria-container > div';
    const rows = fixture.debugElement.queryAll(By.css(selector));
    expect(rows.length).toEqual(2);
  });

  it('Should generate the correct title', () => {
    const title = fixture.debugElement.query(By.css('span.title'));

    typeStub.next(TYPE_ICD9);
    fixture.detectChanges();
    expect(title.nativeElement.textContent).toBe('Selected ICD9 Codes');

    typeStub.next(TYPE_DEMO);
    fixture.detectChanges();
    expect(title.nativeElement.textContent).toBe('Selected demo');

    typeStub.next(null);
    fixture.detectChanges();
    expect(title.nativeElement.textContent).toBe('No Selection');
  });

  it('should dispatch UNSELECT_CRITERIA on removal click', () => {
    typeStub.next(TYPE_ICD9);
    listStub.next(SELECTION_ICD9);
    fixture.detectChanges();

    const selector = 'div#wizard-criteria-container button.text-danger';
    const button = fixture.debugElement.query(By.css(selector));
    button.triggerEventHandler('click', null);

    expect(dispatchSpy).toHaveBeenCalledWith({
      type: UNSELECT_CRITERIA,
      criterionId: 'CodeA',
      criterion: undefined,
    });
  });

});
