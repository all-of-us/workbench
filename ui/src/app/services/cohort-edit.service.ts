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

  add(newCohort: Cohort): Promise<Cohort[]> {
    newCohort.id = this.COHORT.length.toString();
    newCohort.creationTime = new Date();
    this.COHORT.push(newCohort);
    return this.list();
  }



  edit(id: string, newCohort: Cohort): Promise<Cohort[]> {
    for (let coho of this.COHORT) {
      if (coho.id === id) {
        coho = newCohort;
        return this.list();
      }
    }
    return Promise.reject(`No Cohort with ID ${id}.`);
  }
}
