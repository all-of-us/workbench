import {async as _async, ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {DebugElement} from '@angular/core';
import {ClarityModule} from 'clarity-angular';
import {NgRedux, dispatch} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {Map, List, fromJS} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
  openWizard,
  OPEN_WIZARD,
  removeGroup,
  REMOVE_GROUP,
} from '../redux';
import {SearchGroupComponent} from './search-group.component';
import {SearchGroupItemComponent} from '../search-group-item/search-group-item.component';
import {CohortBuilderService} from 'generated';

const itemA = fromJS({
  id: 'itemA',
  count: null,
  isRequesting: false,
  type: 'icd9',
  searchParameters: [],
  modifiers: [],
});

const itemB = fromJS({
  id: 'itemB',
  count: null,
  isRequesting: false,
  type: 'icd9',
  searchParameters: [],
  modifiers: [],
});

const group = fromJS({
  id: 'include0',
  count: null,
  isRequesting: false,
  items: ['itemA', 'itemB'],
});

class MockActions {
  @dispatch() removeGroup = removeGroup;
  @dispatch() openWizard = openWizard;

  generateId(prefix?: string): string {
    return 'TestId';
  }
}

describe('SearchGroupComponent', () => {
  let fixture: ComponentFixture<SearchGroupComponent>;
  let comp: SearchGroupComponent;

  let mockReduxInst;

  beforeEach(_async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed
      .configureTestingModule({
        declarations: [
          SearchGroupComponent,
          SearchGroupItemComponent,
        ],
        imports: [
          ClarityModule,
        ],
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

    fixture = TestBed.createComponent(SearchGroupComponent);
    comp = fixture.componentInstance;

    // Default Inputs for tests
    comp.group = group;
    comp.role = 'includes';
    fixture.detectChanges();
  });

  it('Should render', () => {
    // sanity check
    expect(comp).toBeTruthy();
    const items = fixture.debugElement.queryAll(By.css('app-search-group-item'));
    expect(items.length).toBe(2);
  });

  it('Should dispatch WIZARD_OPEN when a Criteria is selected', () => {
    const spy = spyOn(mockReduxInst, 'dispatch');
    comp.launchWizard('icd9');
    expect(spy).toHaveBeenCalledWith({
      type: OPEN_WIZARD,
      itemId: 'TestId',
      context: {
        criteriaType: 'icd9',
        role: 'includes',
        groupId: 'include0',
        itemId: 'TestId',
      }
    });
  });

  it('Should dispatch REMOVE_GROUP on remove button click', () => {
    const spy = spyOn(mockReduxInst, 'dispatch');
    const button = fixture.debugElement.query(By.css('button.close'));
    button.triggerEventHandler('click', null);
    expect(spy).toHaveBeenCalledWith({
      type: REMOVE_GROUP,
      role: 'includes',
      groupId: 'include0'
    });
  });

  it('Should render zero if no group count', () => {
    const footer = fixture.debugElement.query(By.css('div.card-footer'));
    const spinner = fixture.debugElement.query(By.css('span.spinner'));
    const text = footer.nativeElement.textContent.replace(/\s+/g, ' ').trim();

    expect(text).toEqual('Group Count: 0');
    expect(spinner).toBeNull();
  });

  it('Should render group count if group count', () => {
    comp.group = group.set('count', 25);
    fixture.detectChanges();

    const footer = fixture.debugElement.query(By.css('div.card-footer'));
    const spinner = fixture.debugElement.query(By.css('span.spinner'));
    const text = footer.nativeElement.textContent.replace(/\s+/g, ' ').trim();

    expect(text).toEqual('Group Count: 25');
    expect(spinner).toBeNull();
  });

  it('Should render a spinner if requesting', () => {
    comp.group = group.set('isRequesting', true);
    fixture.detectChanges();
    const spinner = fixture.debugElement.query(By.css('span.spinner'));
    expect(spinner).not.toBeNull();
  });
});
