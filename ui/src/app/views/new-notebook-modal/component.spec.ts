import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';

import {ClarityModule} from '@clr/angular';

import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {
  updateAndTick
} from '../../../testing/test-helpers';

import {ServerConfigService} from 'app/services/server-config.service';
import {SignInService} from 'app/services/sign-in.service';

import {SignInServiceStub} from 'testing/stubs/sign-in-service-stub';

import {NewNotebookModalComponent} from '../new-notebook-modal/component';

describe('NewNotebookModalComponent', () => {
  let fixture: ComponentFixture<NewNotebookModalComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        ClarityModule.forRoot(),
        FormsModule
      ],
      declarations: [
        NewNotebookModalComponent
      ],
      providers: [
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
        {provide: SignInService, useValue: new SignInServiceStub()}
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(NewNotebookModalComponent);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
