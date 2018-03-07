import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';
import {SetAnnotationItemComponent} from './set-annotation-item.component';

import {
  queryByCss,
  updateAndTick,
} from 'testing/test-helpers';

import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
  ModifyCohortAnnotationDefinitionRequest,
} from 'generated';


class StubRoute {
  snapshot = {params: {
    ns: 'workspaceNamespace',
    wsid: 'workspaceId',
    cid: 1
  }};
}

const stubRoute = new StubRoute();

const stubDefinition = <CohortAnnotationDefinition>{
  cohortAnnotationDefinitionId: 1,
  cohortId: 1,
  columnName: 'Test Defn',
  annotationType: AnnotationType.STRING,
};


class ApiSpy {
  updateCohortAnnotationDefinition = jasmine.createSpy('updateCohortAnnotationDefinition');
  deleteCohortAnnotationDefinition = jasmine.createSpy('deleteCohortAnnotationDefinition');
  getCohortAnnotationDefinitions = jasmine.createSpy('getCohortAnnotationDefinitions');
}


describe('SetAnnotationItemComponent', () => {
  let fixture: ComponentFixture<SetAnnotationItemComponent>;
  let component: SetAnnotationItemComponent;

  beforeEach(fakeAsync(() => {
    TestBed
      .configureTestingModule({
        declarations: [
          SetAnnotationItemComponent,
        ],
        imports: [
          ClarityModule,
          ReactiveFormsModule,
        ],
        providers: [
          ReviewStateService,
          {provide: CohortAnnotationDefinitionService, useValue: new ApiSpy()},
          {provide: ActivatedRoute, useValue: stubRoute},
        ],
      }).compileComponents().then((resp) => {
        fixture = TestBed.createComponent(SetAnnotationItemComponent);

        component = fixture.componentInstance;

        // Default Inputs for tests
        component.definition = stubDefinition;
        updateAndTick(fixture);
      });
  }));

  it('Should render', () => {
    expect(component).toBeTruthy();
  });

  it('Should set the form focus when editing', fakeAsync(() => {
    component.editing = true;
    updateAndTick(fixture);
    component.setFocus();
    // component.setFocus() calls setTimeout so there's still a timer on the stack
    // without this call to tick
    tick();
    const inpElem = component.nameInput.nativeElement;
    expect(inpElem).toBeTruthy();
    // Query By css doesn't work here for some reason; just use the real DOM api
    const focusElem = document.activeElement;
    expect(focusElem).toBeTruthy();
    expect(inpElem).toBe(focusElem);
  }));

  it('Should set the form value to the existing definition value', fakeAsync(() => {
    // Monkey patch this method b/c it misbehaves in the test environment
    // unless treated in isolation
    component.setFocus = () => {};
    updateAndTick(fixture);
    component.edit();
    expect(component.definition.columnName).toEqual(component.name.value);
  }));

  it('Should refuse to save an empty name value', fakeAsync(() => {
    // Set up an API spy
    const spy = fixture.debugElement.injector.get(CohortAnnotationDefinitionService) as any;
    // Make sure the form input exists
    component.editing = true;
    updateAndTick(fixture);
    // Set the name to something invalid (e.g. the empty string)
    component.name.setValue('');
    updateAndTick(fixture);
    // Save edit should refuse to call the API
    component.saveEdit();
    expect(spy.updateCohortAnnotationDefinition).not.toHaveBeenCalled();
  }));

  it('Should refuse to save if the name value has not changed', fakeAsync(() => {
    // Set up an API spy
    const spy = fixture.debugElement.injector.get(CohortAnnotationDefinitionService) as any;
    // Make sure the form input exists
    component.editing = true;
    updateAndTick(fixture);
    // ... and then don't do anything (leave the name equal to the form value)
    // Save edit should refuse to call the API
    component.saveEdit();
    expect(spy.updateCohortAnnotationDefinition).not.toHaveBeenCalled();
  }));

  it('Should cancel the edit on ESC', fakeAsync(() => {
    // Set up an API spy
    const spy = fixture.debugElement.injector.get(CohortAnnotationDefinitionService) as any;
    // Make sure the form input exists
    component.editing = true;
    updateAndTick(fixture);
    // Alter the value
    component.name.setValue('Some test value');
    // Dispatch the keypress event
    const input = component.nameInput.nativeElement;
    const event = new KeyboardEvent('keyup', {'key': 'Escape'});
    input.dispatchEvent(event);
    updateAndTick(fixture);
    // Update was never called
    expect(spy.updateCohortAnnotationDefinition).not.toHaveBeenCalled();
    // We are no longer editing
    expect(component.editing).toBe(false);
    // The input no longer exists
    expect(component.nameInput).not.toBeDefined();
  }));

  it('Should call updateCohortAnnotationDefinition in the normal case', fakeAsync(() => {
    // Set up an API spy
    const spy = fixture.debugElement.injector.get(CohortAnnotationDefinitionService) as any;
    // Make sure the form input exists
    component.editing = true;
    updateAndTick(fixture);

    const testValue = 'Some test value';
    component.name.setValue(testValue);
    const input = component.nameInput.nativeElement;
    const event = new KeyboardEvent('keyup', {'key': 'Enter'});
    input.dispatchEvent(event);
    updateAndTick(fixture);

    expect(spy.updateCohortAnnotationDefinition).toHaveBeenCalledWith(
      stubRoute.snapshot.params.ns,
      stubRoute.snapshot.params.wsid,
      stubRoute.snapshot.params.cid,
      stubDefinition.cohortAnnotationDefinitionId,
      <ModifyCohortAnnotationDefinitionRequest>{columnName: testValue}
    );
  }));
});
