import {Injectable} from '@angular/core';
import {CanDeactivate} from '@angular/router';

export interface CanComponentDeactivate {
  canDeactivate: () => Promise<boolean> | boolean;
}

@Injectable()
export class CanDeactivateGuard implements CanDeactivate<CanComponentDeactivate> {
  canDeactivate(component: CanComponentDeactivate): Promise<boolean> | boolean {
    return component && component.canDeactivate ? component.canDeactivate() : true;
  }
}
