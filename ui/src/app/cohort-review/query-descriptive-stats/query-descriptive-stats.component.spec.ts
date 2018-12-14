import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';
import {QueryDescriptiveStatsComponent} from './query-descriptive-stats.component';
import {ReviewStateService} from '../review-state.service';



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
      providers: [
        {provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
      ]
    })
      .compileComponents();
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
