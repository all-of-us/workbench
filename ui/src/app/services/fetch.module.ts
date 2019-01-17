import { Inject, NgModule } from '@angular/core';

import {
  AuditApi,
  AuditApiFactory,
  AuthDomainApi,
  AuthDomainApiFactory,
  BugReportApi,
  BugReportApiFactory,
  CdrVersionsApi,
  CdrVersionsApiFactory,
  ClusterApi,
  ClusterApiFactory,
  CohortAnnotationDefinitionApi,
  CohortAnnotationDefinitionApiFactory,
  CohortBuilderApi,
  CohortBuilderApiFactory,
  CohortReviewApi,
  CohortReviewApiFactory,
  CohortsApi,
  CohortsApiFactory,
  ConceptsApi,
  ConceptsApiFactory,
  ConceptSetsApi,
  ConceptSetsApiFactory,
  ConfigApi,
  ConfigApiFactory,
  Configuration as FetchConfiguration,
  CronApi,
  CronApiFactory,
  OfflineClusterApi,
  OfflineClusterApiFactory,
  ProfileApi,
  ProfileApiFactory,
  StatusApi,
  StatusApiFactory,
  UserApi,
  UserApiFactory,
  UserMetricsApi,
  UserMetricsApiFactory,
  WorkspacesApi,
  WorkspacesApiFactory,
} from 'generated/fetch';


export const FETCH_API_REF = 'fetchApi';

const BASE_PATH_REF = 'basePath';
const tsFetchDeps: any[] = [
  FetchConfiguration, [new Inject(FETCH_API_REF)], [new Inject(BASE_PATH_REF)]
];

/**
 * This module requires a FETCH_API_REF and FetchConfiguration instance to be
 * provided. Unfortunately typescript-fetch does not provide this module by
 * default, so a new entry will need to be added below for each new API service
 * added to the Swagger interfaces.
 *
 * This module is transitional for the Angular -> React conversion. Once routing
 * switches off Angular, we should generate these API stubs dynamically.
 */
@NgModule({
  imports:      [],
  declarations: [],
  exports:      [],
  providers: [
    {
      provide: BASE_PATH_REF,
      deps: [FetchConfiguration],
      useFactory: (c: FetchConfiguration) => c.basePath
    },
    {
      provide: AuditApi,
      deps: tsFetchDeps,
      useFactory: AuditApiFactory
    },
    {
      provide: AuthDomainApi,
      deps: tsFetchDeps,
      useFactory: AuthDomainApiFactory
    },
    {
      provide: BugReportApi,
      deps: tsFetchDeps,
      useFactory: BugReportApiFactory
    },
    {
      provide: CdrVersionsApi,
      deps: tsFetchDeps,
      useFactory: CdrVersionsApiFactory
    },
    {
      provide: ClusterApi,
      deps: tsFetchDeps,
      useFactory: ClusterApiFactory
    },
    {
      provide: CohortAnnotationDefinitionApi,
      deps: tsFetchDeps,
      useFactory: CohortAnnotationDefinitionApiFactory
    },
    {
      provide: CohortBuilderApi,
      deps: tsFetchDeps,
      useFactory: CohortBuilderApiFactory
    },
    {
      provide: CohortReviewApi,
      deps: tsFetchDeps,
      useFactory: CohortReviewApiFactory
    },
    {
      provide: CohortsApi,
      deps: tsFetchDeps,
      useFactory: CohortsApiFactory
    },
    {
      provide: ConceptSetsApi,
      deps: tsFetchDeps,
      useFactory: ConceptSetsApiFactory
    },
    {
      provide: ConceptsApi,
      deps: tsFetchDeps,
      useFactory: ConceptsApiFactory
    },
    {
      provide: ConfigApi,
      deps: tsFetchDeps,
      useFactory: ConfigApiFactory
    },
    {
      provide: CronApi,
      deps: tsFetchDeps,
      useFactory: CronApiFactory
    },
    {
      provide: OfflineClusterApi,
      deps: tsFetchDeps,
      useFactory: OfflineClusterApiFactory
    },
    {
      provide: ProfileApi,
      deps: tsFetchDeps,
      useFactory: ProfileApiFactory
    },
    {
      provide: StatusApi,
      deps: tsFetchDeps,
      useFactory: StatusApiFactory
    },
    {
      provide: UserApi,
      deps: tsFetchDeps,
      useFactory: UserApiFactory
    },
    {
      provide: UserMetricsApi,
      deps: tsFetchDeps,
      useFactory: UserMetricsApiFactory
    },
    {
      provide: WorkspacesApi,
      deps: tsFetchDeps,
      useFactory: WorkspacesApiFactory
    }
  ]
})
export class FetchModule {}
