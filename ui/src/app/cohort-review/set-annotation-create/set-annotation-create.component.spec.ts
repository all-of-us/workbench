import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {CohortAnnotationDefinitionService} from 'generated';

import {ReviewStateService} from '../review-state.service';
import {SetAnnotationCreateComponent} from './set-annotation-create.component';

describe('SetAnnotationCreateComponent', () => {
  let component: SetAnnotationCreateComponent;
  let fixture: ComponentFixture<SetAnnotationCreateComponent>;
  let route;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ SetAnnotationCreateComponent ],
      imports: [ClarityModule, ReactiveFormsModule],
      providers: [
        {provide: ActivatedRoute, useValue: {}},
        {provide: CohortAnnotationDefinitionService, useValue: {}},
        {provide: ReviewStateService, useValue: {}},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SetAnnotationCreateComponent);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
