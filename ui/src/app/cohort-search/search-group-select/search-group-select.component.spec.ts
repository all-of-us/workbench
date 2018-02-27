import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SearchGroupSelectComponent } from './search-group-select.component';

describe('SearchGroupSelectComponent', () => {
  let component: SearchGroupSelectComponent;
  let fixture: ComponentFixture<SearchGroupSelectComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SearchGroupSelectComponent ]
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
