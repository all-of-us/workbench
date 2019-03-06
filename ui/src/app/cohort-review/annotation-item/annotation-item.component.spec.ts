import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';

import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {CohortAnnotationDefinition, CohortReviewApi, ParticipantCohortAnnotation} from 'generated/fetch';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {AnnotationItemComponent} from './annotation-item.component';

interface Annotation {
  definition: CohortAnnotationDefinition;
  value: ParticipantCohortAnnotation;
}

describe('AnnotationItemComponent', () => {
  let component: AnnotationItemComponent;
  let fixture: ComponentFixture<AnnotationItemComponent>;

  beforeEach(async(() => {
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    TestBed.configureTestingModule({
      declarations: [AnnotationItemComponent, ValidatorErrorsComponent],
      imports: [ClarityModule, ReactiveFormsModule],
      providers: [],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AnnotationItemComponent);
    component = fixture.componentInstance;
    component.annotation = <Annotation> {
      definition: <CohortAnnotationDefinition> {},
      value: <ParticipantCohortAnnotation> {}
    };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
