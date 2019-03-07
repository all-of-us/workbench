import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';

import {ParticipantDemographics} from 'generated/fetch';
import {Observable} from 'rxjs/Observable';
import {from} from 'rxjs/observable/from';

@Injectable()
export class DemographicConceptMapsResolver implements Resolve<ParticipantDemographics> {

  constructor() {}

  resolve(route: ActivatedRouteSnapshot): Observable<ParticipantDemographics> {
    const cdrid = +route.parent.data.workspace.cdrVersionId;

    console.log(`Loading Demographics concept maps from resolver`);
    console.log(`cdr id: ${cdrid}`);
    console.dir(route);

    return from(cohortBuilderApi().getParticipantDemographics(cdrid));
  }
}
