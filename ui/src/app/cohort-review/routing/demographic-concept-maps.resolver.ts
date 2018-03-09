import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {
  CohortBuilderService,
  ConceptIdName,
  ParticipantDemographics,
} from 'generated';

@Injectable()
export class DemographicConceptMapsResolver implements Resolve<ParticipantDemographics> {

  constructor(private builderAPI: CohortBuilderService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ParticipantDemographics> {
    const cdrid = +route.parent.data.workspace.cdrVersionId;

    console.log(`Loading Demographics concept maps from resolver`);
    console.log(`cdr id: ${cdrid}`);
    console.dir(route);

    return this.builderAPI.getParticipantDemographics(cdrid);
  }
}
