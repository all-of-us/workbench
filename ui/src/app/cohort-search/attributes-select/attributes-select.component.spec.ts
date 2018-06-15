import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AttributesSelectComponent } from './attributes-select.component';

describe('AttributesSelectComponent', () => {
  let component: AttributesSelectComponent;
  let fixture: ComponentFixture<AttributesSelectComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AttributesSelectComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AttributesSelectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
