import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';

import {Observable} from 'rxjs/Observable';
import {AnnotationItemComponent} from '../annotation-item/annotation-item.component';
import {ReviewStateService} from '../review-state.service';
import {AnnotationListComponent} from './annotation-list.component';

describe('AnnotationListComponent', () => {
  let component: AnnotationListComponent;
  let fixture: ComponentFixture<AnnotationListComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ AnnotationItemComponent, AnnotationListComponent ],
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
