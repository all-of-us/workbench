import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {CohortBuilderApi} from 'generated/fetch';
import {NgxPopperModule} from 'ngx-popper';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';

import {ListOptionInfoComponent} from 'app/cohort-search/list-option-info/list-option-info.component';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ListSearchBarComponent} from './list-search-bar.component';

describe('ListSearchBarComponent', () => {
  let component: ListSearchBarComponent;
  let fixture: ComponentFixture<ListSearchBarComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ListOptionInfoComponent, ListSearchBarComponent, SafeHtmlPipe ],
      imports: [
        ClarityModule,
        NgxPopperModule,
        ReactiveFormsModule
      ],
      providers: [],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    fixture = TestBed.createComponent(ListSearchBarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
