import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';

import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {AnnotationItemComponent} from 'app/cohort-review/annotation-item/annotation-item.component';
import {ReviewStateService} from 'app/cohort-review/review-state.service';
import {Observable} from 'rxjs/Observable';
import {AnnotationListComponent} from './annotation-list.component';

describe('AnnotationListComponent', () => {
  let component: AnnotationListComponent;
  let fixture: ComponentFixture<AnnotationListComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [AnnotationItemComponent, AnnotationListComponent, ValidatorErrorsComponent],
      imports: [ClarityModule, ReactiveFormsModule],
      providers: [
        {provide: ReviewStateService, useValue: {}},
        {provide: ActivatedRoute, useValue: {}},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AnnotationListComponent);
    component = fixture.componentInstance;
    component.annotations$ = Observable.of([]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
