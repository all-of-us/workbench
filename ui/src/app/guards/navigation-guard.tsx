import {Injectable} from '@angular/core';
import {CanDeactivate} from '@angular/router';
import {navigationGuardStore} from 'app/utils/stores';

@Injectable()
export class NavigationGuard implements CanDeactivate<any> {
  canDeactivate(): Promise<boolean> | boolean {
    const navigationGuardedComponent = navigationGuardStore.get() && navigationGuardStore.get().component;

    if (navigationGuardedComponent) {
      return navigationGuardedComponent.canDeactivate();
    }

    return true;
  }
}
