import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';

import {CohortAnnotationDefinition, CohortReviewService, ParticipantCohortAnnotation} from 'generated';
import {Observable} from 'rxjs/Observable';
import {AnnotationItemComponent} from './annotation-item.component';
import {C} from '@angular/core/src/render3';

interface Annotation {
  definition: CohortAnnotationDefinition;
  value: ParticipantCohortAnnotation;
}

describe('AnnotationItemComponent', () => {
  let component: AnnotationItemComponent;
  let fixture: ComponentFixture<AnnotationItemComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ AnnotationItemComponent ],
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
