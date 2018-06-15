import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AttributesPageComponent } from './attributes-page.component';

describe('AttributesPageComponent', () => {
  let component: AttributesPageComponent;
  let fixture: ComponentFixture<AttributesPageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AttributesPageComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AttributesPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
