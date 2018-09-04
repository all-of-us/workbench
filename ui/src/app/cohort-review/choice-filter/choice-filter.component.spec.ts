import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ChoiceFilterComponent} from './choice-filter.component';

describe('ChoiceFilterComponent', () => {
  let component: ChoiceFilterComponent;
  let fixture: ComponentFixture<ChoiceFilterComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ ChoiceFilterComponent ],
      imports: [ReactiveFormsModule],
      providers: [],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ChoiceFilterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
