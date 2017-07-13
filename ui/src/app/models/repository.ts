// A Curated Data Repository which a user might browse to build a cohort.

import {PermissionLevel} from 'app/models/permission-level';

export class Repository {
  id: number;
  name: string;
  permission: PermissionLevel;  // minimum required to see this repository
}
