import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';

import {ClarityModule} from '@clr/angular';

import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {
  updateAndTick
} from '../../../testing/test-helpers';

import {ServerConfigService} from '../../services/server-config.service';

import {ConfirmDeleteModalComponent} from '../confirm-delete-modal/component';

describe('ConfirmDeleteModalComponent', () => {
  let fixture: ComponentFixture<ConfirmDeleteModalComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        ClarityModule.forRoot()
      ],
      declarations: [
        ConfirmDeleteModalComponent,
      ],
      providers: [
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(ConfirmDeleteModalComponent);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
