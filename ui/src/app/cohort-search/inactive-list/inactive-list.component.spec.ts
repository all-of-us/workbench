import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { InactiveListComponent } from './inactive-list.component';

describe('InactiveListComponent', () => {
  let component: InactiveListComponent;
  let fixture: ComponentFixture<InactiveListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ InactiveListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(InactiveListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
