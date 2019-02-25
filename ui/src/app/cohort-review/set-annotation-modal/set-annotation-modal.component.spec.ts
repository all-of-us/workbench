import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {CohortAnnotationDefinitionService} from 'generated';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';

import {ReviewStateService} from 'app/cohort-review/review-state.service';
import {SetAnnotationCreateComponent} from 'app/cohort-review/set-annotation-create/set-annotation-create.component';
import {SetAnnotationItemComponent} from 'app/cohort-review/set-annotation-item/set-annotation-item.component';
import {SetAnnotationListComponent} from 'app/cohort-review/set-annotation-list/set-annotation-list.component';
import {SetAnnotationModalComponent} from './set-annotation-modal.component';

describe('SetAnnotationModalComponent', () => {
  let component: SetAnnotationModalComponent;
  let fixture: ComponentFixture<SetAnnotationModalComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [
        SetAnnotationCreateComponent,
        SetAnnotationItemComponent,
        SetAnnotationListComponent,
        SetAnnotationModalComponent,
      ],
      imports: [ClarityModule, ReactiveFormsModule],
      providers: [
        {provide: CohortAnnotationDefinitionService, useValue: {}},
        {provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SetAnnotationModalComponent);
    component = fixture.componentInstance;
    component.annotationDefinitions = [];
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
