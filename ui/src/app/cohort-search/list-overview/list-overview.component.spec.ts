import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {fromJS} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {CohortSearchActions} from 'app/cohort-search/redux';
import {ListOverviewComponent} from './list-overview.component';

import {CohortBuilderService, CohortsService} from 'generated';


class MockActions {}


describe('ListOverviewComponent', () => {
  let fixture: ComponentFixture<ListOverviewComponent>;
  let component: ListOverviewComponent;

  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const old = mockReduxInst.getState;
    const wrapped = () => fromJS(old());
    mockReduxInst.getState = wrapped;

    TestBed
      .configureTestingModule({
        declarations: [
          ListOverviewComponent,
        ],
        imports: [
          ClarityModule,
        ],
        providers: [
          {provide: NgRedux, useValue: mockReduxInst},
          {provide: CohortsService, useValue: {}},
          {provide: CohortBuilderService, useValue: {}},
          {provide: CohortSearchActions, useValue: new MockActions()},
        ],
        schemas: [NO_ERRORS_SCHEMA],
      })
      .compileComponents();
  }));

  beforeEach(() => {
    MockNgRedux.reset();

    fixture = TestBed.createComponent(ListOverviewComponent);
    component = fixture.componentInstance;

    // Default Inputs for tests
    component.total$ = Observable.of(0);
    component.isRequesting$ = Observable.of(false);

    fixture.detectChanges();
  });

  it('Should render', () => {
    expect(component).toBeTruthy();
  });
});
