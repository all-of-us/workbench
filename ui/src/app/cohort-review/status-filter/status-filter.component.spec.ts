import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';

import {StatusFilterComponent} from './status-filter.component';

describe('StatusFilterComponent', () => {
  let component: StatusFilterComponent;
  let fixture: ComponentFixture<StatusFilterComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ StatusFilterComponent ],
      imports: [ReactiveFormsModule],
      providers: [],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(StatusFilterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
