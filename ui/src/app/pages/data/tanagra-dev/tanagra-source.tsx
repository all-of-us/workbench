import {
  CohortsV2Api,
  CohortV2,
  ConceptSetsV2Api,
  Configuration,
  ConfigurationParameters,
  CreateCohortRequest,
  ReviewsV2Api,
} from 'tanagra-generated';

import { createContext, useContext, useMemo } from 'react';

function apiForEnvironment<API>(api: { new (c: Configuration): API }) {
  const fn = () => {
    const config: ConfigurationParameters = {
      basePath: '',
    };
    return new api(new Configuration(config));
  };
  return createContext(fn());
}

const CohortsV2ApiContext = apiForEnvironment(CohortsV2Api);

export interface TanagraSource {
  createCohort(
    studyId: string,
    displayName: string,
    description: string,
    underlayName: string
  ): Promise<CohortV2>;
}

export function useTanagraSource(): TanagraSource {
  const cohortsApi = useContext(CohortsV2ApiContext) as CohortsV2Api;
  return useMemo(
    () =>
      new BackendSource(cohortsApi),
    []
  );
}

export class BackendSource implements TanagraSource {
  constructor(private cohortsApi: CohortsV2Api) {}

  async createCohort(
    studyId: string,
    displayName: string,
    description: string,
    underlayName: string
  ): Promise<CohortV2> {
    const createCohortRequest: CreateCohortRequest = {
      studyId,
      cohortCreateInfoV2: {
        displayName,
        description,
        underlayName,
      },
    };
    return await this.cohortsApi.createCohort(createCohortRequest);
  }
}
