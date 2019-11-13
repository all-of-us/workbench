import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortBuilderService} from 'generated';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {SearchGroupSelectComponent} from './search-group-select.component';

describe('SearchGroupSelectComponent', () => {
  let component: SearchGroupSelectComponent;
  let fixture: ComponentFixture<SearchGroupSelectComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ SearchGroupSelectComponent ],
      imports: [ClarityModule],
      providers: [
        {provide: CohortBuilderService, useValue: {}},
      ],
    })
      .compileComponents();
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchGroupSelectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
