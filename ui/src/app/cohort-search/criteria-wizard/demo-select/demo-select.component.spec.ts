import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from '@clr/angular';
import {fromJS, List} from 'immutable';

import {DemoSelectComponent} from './demo-select.component';

import {CohortSearchActions} from '../../redux';

describe('DemoSelectComponent', () => {
  let fixture: ComponentFixture<DemoSelectComponent>;
  let component: DemoSelectComponent;

  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const old = mockReduxInst.getState;
    const wrapped = () => fromJS(old());
    mockReduxInst.getState = wrapped;

    TestBed
      .configureTestingModule({
        declarations: [
          DemoSelectComponent,
        ],
        imports: [
          ClarityModule,
          NoopAnimationsModule,
          ReactiveFormsModule,
        ],
        providers: [
          {provide: NgRedux, useValue: mockReduxInst},
          CohortSearchActions,
        ],
      })
      .compileComponents();
  }));

  beforeEach(() => {
    MockNgRedux.reset();

    fixture = TestBed.createComponent(DemoSelectComponent);
    component = fixture.componentInstance;
    component.options = List();
    component.initialSelection = List();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
