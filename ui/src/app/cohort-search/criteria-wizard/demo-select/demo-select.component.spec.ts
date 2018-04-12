import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DemoSelectComponent } from './demo-select.component';

describe('DemoSelectComponent', () => {
  let component: DemoSelectComponent;
  let fixture: ComponentFixture<DemoSelectComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DemoSelectComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DemoSelectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
