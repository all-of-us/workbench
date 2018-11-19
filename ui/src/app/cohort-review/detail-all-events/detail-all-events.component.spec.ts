import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {CohortReviewService} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';

import {ClearButtonInMemoryFilterComponent} from '../clearbutton-in-memory-filter/clearbutton-in-memory-filter.component';
import {DetailTabTableComponent} from '../detail-tab-table/detail-tab-table.component';
import {DetailAllEventsComponent} from './detail-all-events.component';

describe('DetailAllEventsComponent', () => {
  let component: DetailAllEventsComponent;
  let fixture: ComponentFixture<DetailAllEventsComponent>;
  const activatedRouteStub = {
    data: Observable.of({
      workspace: {
        cdrVersionId: '1'
      },
      cohort: {},
      participant: {}
    })
  };

    beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ ClearButtonInMemoryFilterComponent, DetailAllEventsComponent, DetailTabTableComponent ],
      imports: [ClarityModule, NgxPopperModule],
      providers: [
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
        {provide: ActivatedRoute, useValue: activatedRouteStub},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailAllEventsComponent);
    component = fixture.componentInstance;
    component.columns = [{name: ''}];
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
