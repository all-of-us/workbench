import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {CohortReview} from 'generated';
import {Observable} from 'rxjs/Observable';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';

import {CreateReviewPage} from 'app/cohort-review/create-review-page/create-review-page';
import {ReviewStateService} from 'app/cohort-review/review-state.service';
import {PageLayout} from './page-layout';

describe('PageLayout', () => {
  let component: PageLayout;
  let fixture: ComponentFixture<PageLayout>;
  const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
  const activatedRouteStub = {
    snapshot: {
      data: {
        review: <CohortReview> {}
      }
    },
    data: Observable.of({})
  };
  let route;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [
        CreateReviewPage,
        PageLayout,
      ],
      imports: [ClarityModule, ReactiveFormsModule, RouterTestingModule],
      providers: [
        {provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
        {provide: ActivatedRoute, useValue: activatedRouteStub},
        {provide: Router, useValue: routerSpy},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PageLayout);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
