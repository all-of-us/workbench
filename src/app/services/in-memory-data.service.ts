// An in-memory database to back the fake repository REST API.
// RepositoryService uses Angular's http module to (fake) talk to this API.

import {InMemoryDbService} from 'angular-in-memory-web-api';

import {PermissionLevel} from 'app/models/permission-level'

export class InMemoryDataService implements InMemoryDbService {
  createDb() {
    let repository = [
      {id: 11, permission: PermissionLevel.Public, name: 'Aggregates 2017 Q1'},
      {id: 12, permission: PermissionLevel.Public, name: 'Aggregates 2017 Q2'},
      {id: 13, permission: PermissionLevel.Registered, name: 'CDR 2017 Q1 v1'},
      {id: 14, permission: PermissionLevel.Registered, name: 'CDR 2017 Q1 v2'},
      {id: 15, permission: PermissionLevel.Registered, name: 'CDR 2017 Q1 v3'},
      {id: 16, permission: PermissionLevel.Registered, name: 'CDR 2017 Q2 v1'},
      {id: 19, permission: PermissionLevel.Controlled, name: 'Raw 2017 Q1'},
      {id: 20, permission: PermissionLevel.Controlled, name: 'Raw 2017 Q2'}
    ];
    return {repository};  // The variable name determines the fake API URL.
  }
}
