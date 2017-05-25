import { PermissionLevel } from './permission-level';

export class Repository {
  id: number;
  name: string;
  permission: PermissionLevel;  // minimum required to see this repository
}
