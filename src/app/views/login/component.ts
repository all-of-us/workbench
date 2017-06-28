// View for logging in and switching user / permission levels.

import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';

import {PermissionLevel} from 'app/models/permission-level'
import {User} from 'app/models/user'
import {UserService} from 'app/services/user.service'

@Component({templateUrl: './component.html'})
export class LoginComponent implements OnInit {
  constructor(
      private userService: UserService,
      private router: Router
  ) {}

  user: User;  // currently logged in, may be undefined
  login: User = new User();  // form data
  permissionValues = Object.keys(PermissionLevel).map(k => PermissionLevel[k])
      .filter(v => typeof v === "number") as number[];
  permissionNames = PermissionLevel;

  ngOnInit(): void {
    this.userService.getLoggedInUser()
        .then(user => {
          this.user = user
          if (user) {
            this.login = user
          }
        });
  }

  logIn(): void {
    this.userService.logIn(this.login.name, this.login.permission)
        .then(() => this.goToSelectRepository());
  }

  goToSelectRepository(): void {
    this.router.navigate(['/repository']);
  }

  logOut(): void {
    this.userService.logOut()
        .then(() => this.ngOnInit());
  }
}
