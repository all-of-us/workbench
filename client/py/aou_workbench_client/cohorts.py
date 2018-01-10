from auth import get_authenticated_swagger_client
from config import all_of_us_config
from aou_workbench_client.swagger_client.apis.cohorts_api import CohortsApi

def materialize_cohort(request):
    """Materializes a cohort in the workspace containing this notebook, based
    on the provided MateralizedCohortRequest."""
    client = get_authenticated_swagger_client()
    cohorts_api = CohortsApi(api_client=client)
    return cohorts_api.materialize_cohort(all_of_us_config.workspace_namespace,
                                          all_of_us_config.workspace_id,
                                          request=request)