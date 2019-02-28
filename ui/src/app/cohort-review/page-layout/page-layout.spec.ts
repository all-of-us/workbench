import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {CreateReviewPage} from 'app/cohort-review/create-review-page/create-review-page';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {NavStore} from 'app/utils/navigation';
import {cohortReviewStub} from 'testing/stubs/cohort-review-service-stub';
import {PageLayout} from './page-layout';

describe('PageLayout', () => {
  let component: PageLayout;
  let fixture: ComponentFixture<PageLayout>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [
        CreateReviewPage,
        PageLayout,
      ],
      imports: [ClarityModule, ReactiveFormsModule, RouterTestingModule],
      providers: [],
    })
      .compileComponents();
    NavStore.navigate = jasmine.createSpy('navigate');
    cohortReviewStore.next(cohortReviewStub);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PageLayout);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
