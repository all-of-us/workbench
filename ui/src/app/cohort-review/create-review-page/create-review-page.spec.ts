import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {CohortReviewService} from 'generated';
import {CreateReviewPage} from './create-review-page';

describe('CreateReviewPage', () => {
  let component: CreateReviewPage;
  let fixture: ComponentFixture<CreateReviewPage>;
  const activatedRouteStub = {
    parent: {
      snapshot: {
        data: {
          review: {},
          cohort: {}
        }
      }
    }
  };
  let route;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ CreateReviewPage ],
      imports: [ClarityModule, ReactiveFormsModule, RouterTestingModule.withRoutes([])],
      providers: [
        {provide: CohortReviewService, useValue: {}},
        {provide: ActivatedRoute, useValue: activatedRouteStub},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CreateReviewPage);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
