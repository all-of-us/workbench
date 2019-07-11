import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {fromJS} from 'immutable';

import {CohortSearchActions, initGroup, openWizard} from 'app/cohort-search/redux';
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
});
