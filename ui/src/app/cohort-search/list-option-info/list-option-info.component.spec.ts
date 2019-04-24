import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {NgxPopperModule} from 'ngx-popper';

import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {OptionInfoComponent} from './list-option-info.component';

describe('OptionInfoComponent', () => {
  let component: OptionInfoComponent;
  let fixture: ComponentFixture<OptionInfoComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ OptionInfoComponent, SafeHtmlPipe ],
      imports: [ NgxPopperModule]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(OptionInfoComponent);
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
