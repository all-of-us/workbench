import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {Router} from '@angular/router';

import {ServerConfigService} from '../../services/server-config.service';
import {SignInService} from '../../services/sign-in.service';
import {AccountCreationComponent} from '../account-creation/component';
import {AccountCreationModalsComponent} from '../account-creation-modals/component';
import {LoginComponent} from '../login/component';
import {Subscription} from "rxjs/Subscription";

@Component({
  selector : 'app-account-creation-success',
  styleUrls: ['../../styles/template.css'],
  templateUrl: './component.html'
})
export class AccountCreationSuccessComponent implements OnInit {
  username: string;
  @Input('contactEmail') contactEmail: string;
  gsuiteDomain: string;
  subscription: Subscription;

  @ViewChild(AccountCreationModalsComponent)
  accountCreationModalsComponent: AccountCreationModalsComponent;

  constructor(
    private loginComponent: LoginComponent,
    private account: AccountCreationComponent,
    private router: Router,
    private signInService: SignInService,
    serverConfigService: ServerConfigService,
  ) {
    serverConfigService.getConfig().subscribe((config) => {
      this.gsuiteDomain = config.gsuiteDomain;
    });
    // This is a workaround for ExpressionChangedAfterItHasBeenCheckedError from angular
    setTimeout(() => {
      loginComponent.smallerBackgroundImgSrc = '/assets/images/congrats-female-standing.png';
      loginComponent.backgroundImgSrc = '/assets/images/congrats-female.png';
    }, 0);
    this.username = account.profile.username;
  }

  ngOnInit () {
  }

  signIn(): void {
    this.signInService.signIn();
  }

  receiveMessage($event) {
    this.contactEmail = $event;
  }
}
