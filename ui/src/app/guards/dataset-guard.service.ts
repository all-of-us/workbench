import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  CanActivateChild, Router,
} from '@angular/router';

import {environment} from 'environments/environment';



@Injectable()
export class DataSetGuard implements CanActivate, CanActivateChild {
  constructor(private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    if (!environment.enableDatasetBuilder) {
      this.router.navigate(['workspaces', route.params.ns, route.params.wsid]);
    }
    return environment.enableDatasetBuilder;
  }

  canActivateChild(route: ActivatedRouteSnapshot): boolean {
    if (!environment.enableDatasetBuilder) {
      this.router.navigate(['workspaces', route.params.ns, route.params.wsid]);
    }
    return environment.enableDatasetBuilder;
  }
}
