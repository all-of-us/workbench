// A client user, simulating login via Google or OAuth in a browser.

import { PermissionLevel } from './permission-level';

export class User {
  id: number;
  name: string;
  permission: PermissionLevel;
}
