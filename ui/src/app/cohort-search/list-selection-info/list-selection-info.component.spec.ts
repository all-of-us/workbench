import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {NgxPopperModule} from 'ngx-popper';
import {ListSelectionInfoComponent} from './list-selection-info.component';

describe('ListSelectionInfoComponent', () => {
  let component: ListSelectionInfoComponent;
  let fixture: ComponentFixture<ListSelectionInfoComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ListSelectionInfoComponent],
      imports: [ClarityModule, NgxPopperModule],
      providers: [],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ListSelectionInfoComponent);
    component = fixture.componentInstance;
    component.parameter = {
      code: '',
      conceptId: 903133,
      count: 0,
      domainId: 'Measurement',
      group: false,
      hasAttributes: true,
      id: 316305,
      name: 'Height Detail',
      parameterId: 'param316305',
      parentId: 0,
      predefinedAttributes: null,
      selectable: true,
      subtype: 'HEIGHT',
      type: 'PM'
    };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
