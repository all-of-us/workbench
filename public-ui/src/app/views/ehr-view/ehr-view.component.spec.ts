import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { EhrViewComponent } from './ehr-view.component';

describe('EhrViewComponent', () => {
  let component: EhrViewComponent;
  let fixture: ComponentFixture<EhrViewComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ EhrViewComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EhrViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
