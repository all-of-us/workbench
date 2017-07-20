// Data service used to pass information from the edit cohort page
// to the workspace page.

// TODO (blrubenstein): Remove this service once edits to cohorts
// are done server side.

import {Injectable} from '@angular/core';

import {Cohort} from 'generated';

@Injectable()
export class CohortEditService {
  COHORT: Cohort[] = [];

  list(): Promise<Cohort[]> {
    return Promise.resolve(this.COHORT);
  }

  get(id: string): Promise<Cohort> {
    for (const coho of this.COHORT) {
      if (coho.id === id) {
        return Promise.resolve(coho);
      }
    }
    return Promise.reject(`No Cohort with ID ${id}.`);
  }

  add(): string {
    const coho: Cohort = {id: '', name: '', criteria: '', type: ''};
    coho.id = this.COHORT.length.toString();
    coho.creationTime = new Date();
    this.COHORT.push(coho);
    return coho.id;
  }



  edit(id: string, name: string, description: string): Promise<Cohort[]> {
    for (const coho of this.COHORT) {
      if (coho.id === id) {
        coho.name = name;
        coho.description = description;
        coho.lastModifiedTime = new Date();
        return this.list();
      }
    }
    return Promise.reject(`No Cohort with ID ${id}.`);
  }
}
