import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {CohortAnnotationDefinitionService} from 'generated';

import {ReviewStateService} from 'app/cohort-review/review-state.service';
import {SetAnnotationCreateComponent} from './set-annotation-create.component';

describe('SetAnnotationCreateComponent', () => {
  let component: SetAnnotationCreateComponent;
  let fixture: ComponentFixture<SetAnnotationCreateComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ SetAnnotationCreateComponent ],
      imports: [ClarityModule, ReactiveFormsModule],
      providers: [
        {provide: CohortAnnotationDefinitionService, useValue: {}},
        {provide: ReviewStateService, useValue: {}},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SetAnnotationCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
