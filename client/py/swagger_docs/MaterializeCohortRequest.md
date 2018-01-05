# MaterializeCohortRequest

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**cohort_name** | **str** | The name of a cohort that is to be evaluated. Either this or cohortSpec should be specified  | [optional] 
**cohort_spec** | **str** | JSON representation of a cohort to be evaluated (using the same format used for saved cohorts). Either this or cohortName should be specified  | [optional] 
**status_filter** | [**list[CohortStatus]**](CohortStatus.md) | An array of status values; participants with these statuses will be included. Defaults to [NOT_REVIEWED, INCLUDED, NEEDS_FURTHER_REVIEW] -- everything but EXCLUDED. Only valid for use with cohortName (cohorts saved in the database.)  | [optional] 
**cdr_version_name** | **str** | The name of a CDR version to use when evaluating the cohort; if none is specified, the CDR version currently associated with the workspace will be used  | [optional] 
**page_token** | **str** | Pagination token retrieved from a previous call to materializeCohort; used for retrieving additional pages of results. If this is specified, all other fields on MaterializeCohortRequest apart from pageSize must match the values specified on the request that generated this token.  | [optional] 
**page_size** | **int** | Maximum number of results to return in a response. Defaults to 1000.  | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


