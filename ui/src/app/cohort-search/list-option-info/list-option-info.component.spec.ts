import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {NgxPopperModule} from 'ngx-popper';

import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {ListOptionInfoComponent} from './list-option-info.component';

describe('ListOptionInfoComponent', () => {
  let component: ListOptionInfoComponent;
  let fixture: ComponentFixture<ListOptionInfoComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ListOptionInfoComponent, SafeHtmlPipe ],
      imports: [ NgxPopperModule]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ListOptionInfoComponent);
    component = fixture.componentInstance;
    component.option = {
      code: '385321',
      conceptId: 19101117,
      count: null,
      displayName: 'Bioplex brand of carbenoxolone sodium',
      domainId: '',
      group: false,
      hasAttributes: false,
      id: 332800,
      name: 'Bioplex brand of carbenoxolone sodium',
      parentId: 0,
      path: '',
      selectable: true,
      subtype: 'BRAND',
      type: 'DRUG',
    };
    component.searchTerm = 'carbenoxolone';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
