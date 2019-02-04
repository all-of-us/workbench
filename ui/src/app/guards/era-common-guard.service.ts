import {Injectable} from '@angular/core';
import {
  ActivatedRouteSnapshot, CanActivate, CanActivateChild, Router,
  RouterStateSnapshot
} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {ProfileStorageService} from 'app/services/profile-storage.service';

@Injectable()
export class EraCommonGuard implements CanActivate, CanActivateChild {

  constructor(private router: Router, private profileStorageService: ProfileStorageService) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot):
      Observable<boolean> {
    if (route.routeConfig.path === '' ||
        route.routeConfig.path.startsWith('nih-callback/')) {
      // Leave /admin unguarded in order to allow bootstrapping of verified users.
      return Observable.from([true]);
    }

    return this.profileStorageService.profile$.flatMap((profile) => {
      if (profile.linkedNihUsername === null) {
        this.router.navigate(['/']);
        return Observable.from([false]);
      }
      return Observable.from([true])
    })


  }

  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot):
      Observable<boolean> | Promise<boolean> | boolean {
    return this.canActivate(route, state);
  }
}
