import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from '@clr/angular';
import {fromJS, List, Map} from 'immutable';

import {MultiSelectComponent} from './multi-select.component';

import {CohortSearchActions} from '../redux';

// Default test data
const optA = Map({name: 'A', count: 10, parameterId: 'paramA'});
const optB = Map({name: 'B', count: 10, parameterId: 'paramB'});
const optC = Map({name: 'C', count: 10, parameterId: 'paramC'});
// Default select options - no default selection
const options = List([optA, optB, optC]);

describe('MultiSelectComponent', () => {
  let fixture: ComponentFixture<MultiSelectComponent>;
  let component: MultiSelectComponent;

  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const old = mockReduxInst.getState;
    const wrapped = () => fromJS(old());
    mockReduxInst.getState = wrapped;

    TestBed
      .configureTestingModule({
        declarations: [
          MultiSelectComponent,
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

    fixture = TestBed.createComponent(MultiSelectComponent);
    component = fixture.componentInstance;
    component.options = options;
    component.initialSelection = List();
    fixture.detectChanges();
  });

  // Helper function - lots of the tests below start by setting an option as
  // initially selected
  const initializeWithOptA = () => {
    component.select(optA);
    fixture.detectChanges();
    expect(component.selectedOptions.size).toEqual(1);
    // use immutable's in-house equality func for immutable objects
    expect(component.selectedOptions.get(0).equals(optA)).toBeTruthy();
  };

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should select items', () => {
    expect(component.selectedOptions.size).toEqual(0);
    initializeWithOptA();
  });

  /*
   * In an earlier version, the selection set stored hashes of the parameter
   * objects.  However, the parameter objects are slightly different when
   * generated directly from a criterion and loaded from a previously generated
   * cohort (we map some properties to others, etc), so this failed when
   * cloning workspaces.  The below test is to ensure that we continue to
   * recognize "equivalent" parameters when determining what has and hasn't
   * been selected
   */
  it('should recognize equivalent initial selections', () => {
    // Simulate the normal case - we're editing some parameters we made in the same session
    initializeWithOptA();
    // Simulate the clone case - initially selected optA is slightly different,
    // as if its gone through the API and back
    const newOptA = optA.set('value', 'whatever');

    // newOptA is equivalent but not equal to the original optA
    expect(optA.equals(newOptA)).toBeFalsy();
    expect(optA.hashCode()).not.toEqual(newOptA.hashCode());

    component.initialSelection = List([newOptA]);
    fixture.detectChanges();
    expect(component.selectedOptions.size).toEqual(1);
    expect(component.selectedOptions.get(0).equals(optA)).toBeTruthy();
  });

  it('should merge initial and subsequent selections', () => {
    initializeWithOptA();
    component.select(optB);
    fixture.detectChanges();
    expect(component.selectedOptions.size).toEqual(2);
    expect(component.selectedOptions.equals(List([optA, optB]))).toBeTruthy();
  });

  it('should unselect', () => {
    component.select(optA);
    fixture.detectChanges();
    expect(component.selectedOptions.size).toEqual(1);
    expect(component.selectedOptions.get(0).equals(optA)).toBeTruthy();

    component.unselect(optA);
    fixture.detectChanges();
    expect(component.selectedOptions.size).toEqual(0);
    expect(component.selectedOptions.get(0)).toEqual(undefined);
  });
});
