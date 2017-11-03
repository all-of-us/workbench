import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SearchGroupListComponent } from './search-group-list.component';

describe('SearchGroupListComponent', () => {
  let component: SearchGroupListComponent;
  let fixture: ComponentFixture<SearchGroupListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SearchGroupListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchGroupListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
