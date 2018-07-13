import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DbHomeComponent } from './db-home.component';

describe('DbHomeComponent', () => {
  let component: DbHomeComponent;
  let fixture: ComponentFixture<DbHomeComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DbHomeComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DbHomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
