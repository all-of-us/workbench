import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';
import {SetAnnotationItemComponent} from './set-annotation-item.component';

import {
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
});
