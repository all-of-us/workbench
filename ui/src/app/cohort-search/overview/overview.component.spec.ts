import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, Router} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {fromJS} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {CohortSearchActions} from '../redux';
import {OverviewComponent} from './overview.component';

import {CohortBuilderService, CohortsService} from 'generated';


class MockActions {}


describe('OverviewComponent', () => {
  let fixture: ComponentFixture<OverviewComponent>;
  let component: OverviewComponent;

  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const old = mockReduxInst.getState;
    const wrapped = () => fromJS(old());
    mockReduxInst.getState = wrapped;

    TestBed
      .configureTestingModule({
        declarations: [
          OverviewComponent,
        ],
        imports: [
          ClarityModule,
        ],
        providers: [
          {provide: NgRedux, useValue: mockReduxInst},
          {provide: CohortsService, useValue: {}},
          {provide: CohortBuilderService, useValue: {}},
          {provide: Router, useValue: {}},
          {provide: ActivatedRoute, useValue: {}},
          {provide: CohortSearchActions, useValue: new MockActions()},
        ],
        schemas: [NO_ERRORS_SCHEMA],
      })
      .compileComponents();
  }));

  beforeEach(() => {
    MockNgRedux.reset();

    fixture = TestBed.createComponent(OverviewComponent);
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
