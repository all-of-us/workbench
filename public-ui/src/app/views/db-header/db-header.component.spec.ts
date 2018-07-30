import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DbHeaderComponent } from './db-header.component';

describe('DbHeaderComponent', () => {
  let component: DbHeaderComponent;
  let fixture: ComponentFixture<DbHeaderComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DbHeaderComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DbHeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
