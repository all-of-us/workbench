import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';

import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {AnnotationItemComponent} from 'app/cohort-review/annotation-item/annotation-item.component';
import {AnnotationListComponent} from './annotation-list.component';

describe('AnnotationListComponent', () => {
  let component: AnnotationListComponent;
  let fixture: ComponentFixture<AnnotationListComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [AnnotationItemComponent, AnnotationListComponent, ValidatorErrorsComponent],
      imports: [ClarityModule, ReactiveFormsModule],
      providers: [
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AnnotationListComponent);
    component = fixture.componentInstance;
    component.annotations = [];
    component.annotationDefinitions = [];
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
