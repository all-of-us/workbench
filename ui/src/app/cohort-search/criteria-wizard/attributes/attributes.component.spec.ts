import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async as _async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ClarityModule} from 'clarity-angular';
import {fromJS, Map} from 'immutable';

import {CohortSearchActions} from '../../redux';
import {AttributesComponent} from '../attributes/attributes.component';

import {CohortBuilderService} from 'generated';

class MockActions {}

describe('AttributesComponent', () => {
  let fixture: ComponentFixture<AttributesComponent>;
  let component: AttributesComponent;
  let mockReduxInst;

  beforeEach(_async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed
      .configureTestingModule({
        declarations: [
          AttributesComponent,
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

    fixture = TestBed.createComponent(AttributesComponent);
    component = fixture.componentInstance;
    component.node = Map();
    fixture.detectChanges();
  });

  xit('Should render', () => {
    // Running this test somehow crashes the OTHER tests :(
    // wtf, angular.
    // Need to figure out how to test a component with dynamic children
    expect(component).toBeTruthy();
  });
});
