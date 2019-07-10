import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {wizardStore} from 'app/cohort-search/search-state.service';
import {NgxPopperModule} from 'ngx-popper';
import {ListNodeInfoComponent} from './list-node-info.component';

describe('ListNodeInfoComponent', () => {
  let component: ListNodeInfoComponent;
  let fixture: ComponentFixture<ListNodeInfoComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        ListNodeInfoComponent,
        SafeHtmlPipe,
      ],
      imports: [
        ClarityModule,
        NgxPopperModule,
      ],
      providers: [],
    })
      .compileComponents();
    wizardStore.next({});
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ListNodeInfoComponent);
    component = fixture.componentInstance;
    component.node = {
      code: '',
      conceptId: 903133,
      count: 0,
      domainId: 'Measurement',
      group: false,
      hasAttributes: true,
      id: 316305,
      name: 'Height Detail',
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
