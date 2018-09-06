import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {CohortAnnotationDefinitionService, CohortReviewService} from 'generated';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';

import {ChoiceFilterComponent} from '../choice-filter/choice-filter.component';
import {ReviewNavComponent} from '../review-nav/review-nav.component';
import {ReviewStateService} from '../review-state.service';
import {SetAnnotationCreateComponent} from '../set-annotation-create/set-annotation-create.component';
import {SetAnnotationItemComponent} from '../set-annotation-item/set-annotation-item.component';
import {SetAnnotationListComponent} from '../set-annotation-list/set-annotation-list.component';
import {SetAnnotationModalComponent} from '../set-annotation-modal/set-annotation-modal.component';
import {StatusFilterComponent} from '../status-filter/status-filter.component';
import {TablePage} from './table-page';

describe('TablePage', () => {
  let component: TablePage;
  let fixture: ComponentFixture<TablePage>;
  const activatedRouteStub = {
    snapshot: {
      data: {
        concepts: {
          raceList: [],
          genderList: [],
          ethnicityList: [],
        }
      },
      pathFromRoot: [{data: {workspace: {cdrVersionId: 1}}}]
    }
  };
  let route;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [
        ChoiceFilterComponent,
        ReviewNavComponent,
        TablePage,
        SetAnnotationCreateComponent,
        SetAnnotationItemComponent,
        SetAnnotationListComponent,
        SetAnnotationModalComponent,
        StatusFilterComponent,
      ],
      imports: [ClarityModule, ReactiveFormsModule, RouterTestingModule],
      providers: [
        {
          provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
        {
          provide: ActivatedRoute, useValue: activatedRouteStub},
        {provide: CohortAnnotationDefinitionService, useValue: {}},
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TablePage);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
