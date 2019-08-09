import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {CohortBuilderService} from 'generated';
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
