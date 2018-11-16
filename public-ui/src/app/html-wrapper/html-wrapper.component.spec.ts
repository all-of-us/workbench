import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { HtmlWrapperComponent } from './html-wrapper.component';

describe('HtmlWrapperComponent', () => {
  let component: HtmlWrapperComponent;
  let fixture: ComponentFixture<HtmlWrapperComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ HtmlWrapperComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HtmlWrapperComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
