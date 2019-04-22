import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {ClarityModule} from '@clr/angular';
import {fromJS, List} from 'immutable';
import {NgxPopperModule} from 'ngx-popper';

import {
  CohortSearchActions,
  CohortSearchState,
  getItem,
  parameterList,
  REOPEN_WIZARD,
} from 'app/cohort-search/redux';
import {ListSearchGroupItemComponent} from './list-search-group-item.component';

import {CohortBuilderService, TreeType} from 'generated';

const baseItem = fromJS({
  id: 'item001',
  type: TreeType[TreeType.ICD9],
  searchParameters: [0, 1],
  modifiers: [],
  count: null,
  isRequesting: false,
  status: 'active',
});

const zeroCrit = fromJS({
  id: 0,
  type: TreeType[TreeType.ICD9],
  code: 'CodeA',
});

const oneCrit = fromJS({
  id: 1,
  type: TreeType[TreeType.ICD9],
  code: 'CodeB',
});


describe('ListSearchGroupItemComponent', () => {
  let fixture: ComponentFixture<ListSearchGroupItemComponent>;
  let comp: ListSearchGroupItemComponent;

  let mockReduxInst;
  let itemStub;
  let codeStub;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const old = mockReduxInst.getState;
    const wrapped = () => fromJS(old());
    mockReduxInst.getState = wrapped;

    TestBed
      .configureTestingModule({
        declarations: [ListSearchGroupItemComponent],
        imports: [
          ClarityModule,
          NgxPopperModule,
        ],
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

    fixture = TestBed.createComponent(ListSearchGroupItemComponent);
    comp = fixture.componentInstance;

    // Default Inputs for tests
    comp.role = 'includes';
    comp.groupId = 'include0';

    comp.itemId = 'item001';

    itemStub = MockNgRedux
      .getSelectorStub<CohortSearchState, any>(
        getItem(comp.itemId));

    codeStub = MockNgRedux
      .getSelectorStub<CohortSearchState, List<any>>(
        parameterList(comp.itemId));

    fixture.detectChanges();
  });

  it('Should display code type', () => {
    itemStub.next(baseItem);
    codeStub.next(List([zeroCrit, oneCrit]));
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('small.trigger'))).toBeTruthy();

    const display = fixture.debugElement.query(By.css('small.trigger')).nativeElement;
    expect(display.childElementCount).toBe(2);

    const trimmedText = display.textContent.replace(/\s+/g, ' ').trim();
    expect(trimmedText).toEqual('Contains ICD9 Codes');
  });

  it('Should properly pluralize \'Code\'', () => {
    const critList = List([zeroCrit]);
    codeStub.next(critList);
    fixture.detectChanges();
    expect(comp.pluralizedCode).toBe('Code');

    codeStub.next(critList.push(oneCrit));
    fixture.detectChanges();
    expect(comp.pluralizedCode).toBe('Codes');
  });

  it('Should dispatch REOPEN_WIZARD on edit', () => {
    /*
     * More specifically, when the edit icon is clicked, it should dispatch an
     * action like {type: REOPEN_WIZARD, item, context}
     */
    const spy = spyOn(mockReduxInst, 'dispatch');
    itemStub.next(baseItem);
    codeStub.next(List([zeroCrit, oneCrit]));
    fixture.detectChanges();

    const expectedContext = {
      criteriaType: TreeType[TreeType.ICD9],
      criteriaSubtype: null,
      role: 'includes',
      groupId: 'include0',
      itemId: 'item001',
      fullTree: false,
      codes: false
    };

    const dropdown = fixture.debugElement.query(By.css('.dropdown-toggle'));
    dropdown.triggerEventHandler('click', null);

    const editButton = fixture.debugElement.query(By.css('button[clrdropdownitem]:first-of-type'));
    editButton.triggerEventHandler('click', null);

    expect(spy).toHaveBeenCalledWith({
      type: REOPEN_WIZARD,
      item: baseItem,
      context: expectedContext,
    });
  });

});
