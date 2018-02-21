import {NgZone} from '@angular/core';
import {async, ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';
import {SetAnnotationItemComponent} from './set-annotation-item.component';

import {
  updateAndTick
} from 'testing/test-helpers';

import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
} from 'generated';

class StubRoute {
  snapshot = {params: {
    ns: 'workspaceNamespace',
    wsid: 'workspaceId',
    cid: 1
  }};
}


/*
 * The test as written actually passes. Uncommenting various combinations of
 * (A) (B) and (C), however, leads to various unhelpful and confusing error
 * messages.  Whether the custom provider in (B) is just a blank object, or a
 * more robustly written class, or any other value, doesn't seem to matter.
 */

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
          // (A)
          // NgZone,
          // (B)
          // {provide: NgZone, useValue: NgZone},
          ReviewStateService,
          {provide: CohortAnnotationDefinitionService, useValue: {}},
          {provide: ActivatedRoute, useClass: StubRoute},
        ],
      }).compileComponents().then((resp) => {
        fixture = TestBed.createComponent(SetAnnotationItemComponent);

        tick();
        component = fixture.componentInstance;

        // Default Inputs for tests
        component.definition = <CohortAnnotationDefinition>{
          cohortAnnotationDefinitionId: 1,
          cohortId: 1,
          columnName: 'Test Defn',
          annotationType: AnnotationType.STRING,
        };
        updateAndTick(fixture);
      });
      tick();

  }));

  it('Should render', fakeAsync(() => {
    expect(component).toBeTruthy();
    // (C)
    component.editing = true;
    fixture.detectChanges();
    component.edit();
    tick();
  }));
});
