import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {CohortReviewService, WorkspaceAccessLevel} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import 'rxjs/add/observable/of';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';

import {ClearButtonInMemoryFilterComponent} from 'app/cohort-review/clearbutton-in-memory-filter/clearbutton-in-memory-filter.component';
import {DetailTabTableComponent} from 'app/cohort-review/detail-tab-table/detail-tab-table.component';
import {currentCohortStore, currentWorkspaceStore} from 'app/utils/navigation';
import {DetailAllEventsComponent} from './detail-all-events.component';

describe('DetailAllEventsComponent', () => {
  let component: DetailAllEventsComponent;
  let fixture: ComponentFixture<DetailAllEventsComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ ClearButtonInMemoryFilterComponent,
        DetailAllEventsComponent, DetailTabTableComponent],
      imports: [ClarityModule, NgxPopperModule, ReactiveFormsModule, FormsModule],
      providers: [
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
      ],
    })
      .compileComponents();
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
    currentCohortStore.next({
      name: '',
      criteria: '{}',
      type: '',
    });
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
