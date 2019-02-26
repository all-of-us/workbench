import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {CohortAnnotationDefinitionService} from 'generated';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';

import {ReviewStateService} from 'app/cohort-review/review-state.service';
import {SetAnnotationItemComponent} from 'app/cohort-review/set-annotation-item/set-annotation-item.component';
import {SetAnnotationListComponent} from './set-annotation-list.component';

describe('SetAnnotationListComponent', () => {
  let component: SetAnnotationListComponent;
  let fixture: ComponentFixture<SetAnnotationListComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ SetAnnotationItemComponent, SetAnnotationListComponent ],
      imports: [ClarityModule, ReactiveFormsModule],
      providers: [
        {provide: CohortAnnotationDefinitionService, useValue: {}},
        {provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SetAnnotationListComponent);
    component = fixture.componentInstance;
    component.definitions = [];
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
