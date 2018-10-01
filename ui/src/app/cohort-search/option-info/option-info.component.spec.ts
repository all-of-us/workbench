import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { OptionInfoComponent } from './option-info.component';

describe('OptionInfoComponent', () => {
  let component: OptionInfoComponent;
  let fixture: ComponentFixture<OptionInfoComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ OptionInfoComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(OptionInfoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
