import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {CohortBuilderService} from 'generated';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {QueryCohortDefinitionComponent} from '../query-cohort-definition/query-cohort-definition.component';


describe('QueryCohortDefinitionComponent', () => {
  let component: QueryCohortDefinitionComponent;
  let fixture: ComponentFixture<QueryCohortDefinitionComponent>;


  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        QueryCohortDefinitionComponent,
      ],
      providers: [
        {provide: CohortBuilderService, useValue: new CohortBuilderServiceStub()},
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(QueryCohortDefinitionComponent);
    component = fixture.componentInstance;
    // route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
