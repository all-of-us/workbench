import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {ClearButtonInMemoryFilterComponent} from 'app/cohort-review/clearbutton-in-memory-filter/clearbutton-in-memory-filter.component';
import {currentCohortStore, currentWorkspaceStore} from 'app/utils/navigation';
import {CohortReviewService, WorkspaceAccessLevel} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {DetailTabTableComponent} from './detail-tab-table.component';

describe('DetailTabTableComponent', () => {
  let component: DetailTabTableComponent;
  let fixture: ComponentFixture<DetailTabTableComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ ClearButtonInMemoryFilterComponent, DetailTabTableComponent ],
      imports: [ClarityModule, NgxPopperModule, FormsModule, ReactiveFormsModule],
      providers: [
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
      ],
    })
      .compileComponents();
    currentCohortStore.next({
      name: '',
      criteria: '',
      type: '',
    });
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      cdrVersionId: '1',
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailTabTableComponent);
    component = fixture.componentInstance;
    component.columns = [{name: ''}];
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
