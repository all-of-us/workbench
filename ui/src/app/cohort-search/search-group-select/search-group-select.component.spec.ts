import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {fromJS} from 'immutable';

import {
  CohortSearchActions,
  INIT_SEARCH_GROUP,
  initGroup,
  OPEN_WIZARD,
  openWizard,
} from '../redux';

import {CohortBuilderService} from 'generated';
import {SearchGroupSelectComponent} from './search-group-select.component';

class MockActions {
  @dispatch() initGroup = initGroup;
  @dispatch() openWizard = openWizard;

  generateId(prefix?: string): string {
    return 'Test' + prefix;
  }
}

describe('SearchGroupSelectComponent', () => {
  let component: SearchGroupSelectComponent;
  let fixture: ComponentFixture<SearchGroupSelectComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [ SearchGroupSelectComponent ],
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
    fixture = TestBed.createComponent(SearchGroupSelectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('Should dispatch INIT_SEARCH_GROUP on Add Group button click', () => {
    const spy = spyOn(mockReduxInst, 'dispatch');
    component.role = 'includes';
    fixture.detectChanges();
    component.launchWizard('test');
    expect(spy).toHaveBeenCalledWith({
      type: INIT_SEARCH_GROUP,
      role: 'includes',
      groupId: 'Testincludes',
    });
    expect(spy).toHaveBeenCalledWith({
      type: OPEN_WIZARD,
      itemId: 'Testitems',
      context: {
        criteriaType: 'test',
        role: 'includes',
        groupId: 'Testincludes',
        itemId: 'Testitems'
      },
    });
  });

});
