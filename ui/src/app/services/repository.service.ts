// Data interface for getting details about available CDRs. This fake service
// uses hardcoded data for demo purposes.

import {Injectable} from '@angular/core';

import {PermissionLevel} from 'app/models/permission-level';
import {Repository} from 'app/models/repository';

@Injectable()
export class RepositoryService {
  REPOSITORY: Repository[] = [
    {id: 11, permission: PermissionLevel.Public, name: 'Aggregates 2017 Q1'},
    {id: 12, permission: PermissionLevel.Public, name: 'Aggregates 2017 Q2'},
    {id: 13, permission: PermissionLevel.Registered, name: 'CDR 2017 Q1 v1'},
    {id: 14, permission: PermissionLevel.Registered, name: 'CDR 2017 Q1 v2'},
    {id: 15, permission: PermissionLevel.Registered, name: 'CDR 2017 Q1 v3'},
    {id: 16, permission: PermissionLevel.Registered, name: 'CDR 2017 Q2 v1'},
    {id: 19, permission: PermissionLevel.Controlled, name: 'Raw 2017 Q1'},
    {id: 20, permission: PermissionLevel.Controlled, name: 'Raw 2017 Q2'}
  ];

  list(): Promise<Repository[]> {
    return Promise.resolve(this.REPOSITORY);
  }

  get(id: number): Promise<Repository> {
    for (const repo of this.REPOSITORY) {
      if (repo.id === id) {
        return Promise.resolve(repo);
      }
    }
    return Promise.reject(`No Repository with ID ${id}.`);
  }
}
