import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {CohortReviewService, ParticipantDataListResponse} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';

import {DetailTabTableComponent} from './detail-tab-table.component';

describe('DetailTabTableComponent', () => {
  let component: DetailTabTableComponent;
  let fixture: ComponentFixture<DetailTabTableComponent>;
  const activatedRouteStub = {
    data: Observable.of({
      workspace: {cdrVersionId: '1'},
      cohort: {},
      participant: {}
    })
  };
  let route;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ DetailTabTableComponent ],
      imports: [ClarityModule, NgxPopperModule],
      providers: [
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
        {provide: ActivatedRoute, useValue: activatedRouteStub},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailTabTableComponent);
    component = fixture.componentInstance;
    component.columns = [{name: ''}];
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
