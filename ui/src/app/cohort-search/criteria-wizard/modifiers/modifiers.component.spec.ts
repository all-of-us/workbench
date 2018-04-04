import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ModifiersComponent } from './modifiers.component';

describe('ModifiersComponent', () => {
  let component: ModifiersComponent;
  let fixture: ComponentFixture<ModifiersComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ModifiersComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ModifiersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
