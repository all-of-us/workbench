import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {wizardStore} from 'app/cohort-search/search-state.service';
import {NgxPopperModule} from 'ngx-popper';
import {NodeInfoComponent} from './node-info.component';

describe('NodeInfoComponent', () => {
  let component: NodeInfoComponent;
  let fixture: ComponentFixture<NodeInfoComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        NodeInfoComponent,
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
    fixture = TestBed.createComponent(NodeInfoComponent);
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
