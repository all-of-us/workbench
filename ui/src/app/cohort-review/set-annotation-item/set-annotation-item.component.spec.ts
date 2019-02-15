import {ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';

import {ReviewStateService} from 'app/cohort-review/review-state.service';
import {urlParamsStore} from 'app/utils/navigation';
import {SetAnnotationItemComponent} from './set-annotation-item.component';

import {
  updateAndTick,
} from 'testing/test-helpers';

import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
} from 'generated';

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
        ],
      }).compileComponents().then((resp) => {
        fixture = TestBed.createComponent(SetAnnotationItemComponent);

        component = fixture.componentInstance;

        // Default Inputs for tests
        component.definition = stubDefinition;
        updateAndTick(fixture);
      });
    urlParamsStore.next({
      ns: 'workspaceNamespace',
      wsid: 'workspaceId',
      cid: 1
    });
  }));

  it('Should render', () => {
    expect(component).toBeTruthy();
  });
});
