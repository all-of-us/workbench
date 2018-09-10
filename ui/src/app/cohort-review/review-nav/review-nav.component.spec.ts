import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import {ReviewNavComponent} from './review-nav.component';

describe('ReviewNavComponent', () => {
  let component: ReviewNavComponent;
  let fixture: ComponentFixture<ReviewNavComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ ReviewNavComponent ],
      imports: [],
      providers: [],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ReviewNavComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
