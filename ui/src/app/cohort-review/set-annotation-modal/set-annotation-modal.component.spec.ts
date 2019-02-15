import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {CohortAnnotationDefinitionService} from 'generated';
import {cohortAnnotationDefinitionStub} from 'testing/stubs/cohort-annotation-definition-service-stub';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';

import {annotationDefinitionsStore, ReviewStateService} from 'app/cohort-review/review-state.service';
import {SetAnnotationCreateComponent} from 'app/cohort-review/set-annotation-create/set-annotation-create.component';
import {SetAnnotationItemComponent} from 'app/cohort-review/set-annotation-item/set-annotation-item.component';
import {SetAnnotationListComponent} from 'app/cohort-review/set-annotation-list/set-annotation-list.component';
import {SetAnnotationModalComponent} from './set-annotation-modal.component';

describe('SetAnnotationModalComponent', () => {
  let component: SetAnnotationModalComponent;
  let fixture: ComponentFixture<SetAnnotationModalComponent>;
  let route;

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
        {provide: ActivatedRoute, useValue: {}},
        {provide: CohortAnnotationDefinitionService, useValue: {}},
        {provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
      ],
    })
      .compileComponents();
    annotationDefinitionsStore.next([cohortAnnotationDefinitionStub]);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SetAnnotationModalComponent);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
