import {async as _async, ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {DebugElement} from '@angular/core';
import {ClarityModule} from 'clarity-angular';
import {NgRedux, dispatch} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {List, fromJS} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {CohortSearchActions} from '../redux';
import {SearchGroupComponent} from '../search-group/search-group.component';
import {SearchGroupItemComponent} from '../search-group-item/search-group-item.component';
import {CohortBuilderService} from 'generated';

import {SearchGroupListComponent} from './search-group-list.component';

class MockActions {
}

describe('SearchGroupListComponent', () => {
  let fixture: ComponentFixture<SearchGroupListComponent>;
  let component: SearchGroupListComponent;

  let mockReduxInst;

  beforeEach(_async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed
      .configureTestingModule({
        declarations: [
          SearchGroupListComponent,
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

    fixture = TestBed.createComponent(SearchGroupListComponent);
    component = fixture.componentInstance;

    // Default Inputs for tests
    component.role = 'includes';
    component.groups$ = Observable.of(List());

    fixture.detectChanges();
  });

  it('Should render', () => {
    expect(component).toBeTruthy();
  });
});
