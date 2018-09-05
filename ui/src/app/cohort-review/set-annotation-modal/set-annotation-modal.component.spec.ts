import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {AnnotationType, CohortAnnotationDefinition, CohortAnnotationDefinitionService} from 'generated';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';
import {SetAnnotationCreateComponent} from '../set-annotation-create/set-annotation-create.component';
import {SetAnnotationItemComponent} from '../set-annotation-item/set-annotation-item.component';
import {SetAnnotationListComponent} from '../set-annotation-list/set-annotation-list.component';
import {SetAnnotationModalComponent} from './set-annotation-modal.component';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

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
        {provide: ReviewStateService, useValue: {
          annotationManagerOpen: new BehaviorSubject<boolean>(false),
          editAnnotationManagerOpen: new BehaviorSubject<boolean>(false),
          annotationDefinitions$: Observable.of([
            <CohortAnnotationDefinition> {
              cohortAnnotationDefinitionId: 1,
              cohortId: 2,
              columnName: 'test',
              annotationType: AnnotationType.BOOLEAN
            }
          ]),
        }},
      ],
    })
      .compileComponents();
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
