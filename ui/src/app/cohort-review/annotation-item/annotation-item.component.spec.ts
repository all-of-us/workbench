import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';

import {CohortAnnotationDefinition, CohortReviewService, ParticipantCohortAnnotation} from 'generated';
import {ValidatorErrorsComponent} from '../../cohort-common/validator-errors/validator-errors.component';
import {AnnotationItemComponent} from './annotation-item.component';

interface Annotation {
  definition: CohortAnnotationDefinition;
  value: ParticipantCohortAnnotation;
}

describe('AnnotationItemComponent', () => {
  let component: AnnotationItemComponent;
  let fixture: ComponentFixture<AnnotationItemComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [AnnotationItemComponent, ValidatorErrorsComponent],
      imports: [ClarityModule, ReactiveFormsModule],
      providers: [
        {provide: CohortReviewService, useValue: {}},
        {provide: ActivatedRoute, useValue: {}},
      ],
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
