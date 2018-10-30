import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute} from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {CohortReviewService} from 'generated';
import {CreateReviewModal} from './create-review-modal';

describe('CreateReviewModal', () => {
  let component: CreateReviewModal;
  let fixture: ComponentFixture<CreateReviewModal>;
  const activatedRouteStub = {
    snapshot: {
      data: {
        review: {},
        cohort: {}
      }
    }
  };
  let route;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ CreateReviewModal ],
      imports: [
        BrowserAnimationsModule,
        ClarityModule,
        ReactiveFormsModule,
        RouterTestingModule.withRoutes([])
      ],
      providers: [
        {provide: CohortReviewService, useValue: {}},
        {provide: ActivatedRoute, useValue: activatedRouteStub},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CreateReviewModal);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
