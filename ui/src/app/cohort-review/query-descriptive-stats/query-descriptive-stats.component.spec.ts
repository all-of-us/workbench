import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {cohortReviewStub} from 'testing/stubs/cohort-review-service-stub';
import {QueryDescriptiveStatsComponent} from './query-descriptive-stats.component';




describe('QueryDescriptiveStatsComponent', () => {
  let component: QueryDescriptiveStatsComponent;
  let fixture: ComponentFixture<QueryDescriptiveStatsComponent>;


  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        QueryDescriptiveStatsComponent,
      ],
      imports: [
        ClarityModule,
      ],
      providers: []
    })
      .compileComponents();
    cohortReviewStore.next(cohortReviewStub);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(QueryDescriptiveStatsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
