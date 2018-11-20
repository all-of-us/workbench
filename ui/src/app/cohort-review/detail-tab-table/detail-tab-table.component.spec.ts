import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {CohortReviewService} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {ClearButtonInMemoryFilterComponent} from '../clearbutton-in-memory-filter/clearbutton-in-memory-filter.component';
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
      declarations: [ ClearButtonInMemoryFilterComponent, DetailTabTableComponent ],
      imports: [ClarityModule, NgxPopperModule, FormsModule, ReactiveFormsModule],
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
