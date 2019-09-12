import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {CohortBuilderApi} from 'generated/fetch';
import {NgxPopperModule} from 'ngx-popper';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';

import {OptionInfoComponent} from 'app/cohort-search/option-info/option-info.component';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {SearchBarComponent} from './search-bar.component';

describe('SearchBarComponent', () => {
  let component: SearchBarComponent;
  let fixture: ComponentFixture<SearchBarComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ OptionInfoComponent, SearchBarComponent, SafeHtmlPipe ],
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
    fixture = TestBed.createComponent(SearchBarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
