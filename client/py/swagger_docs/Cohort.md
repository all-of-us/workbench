# Cohort

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **int** |  | [optional] 
**etag** | **str** | Entity tag for optimistic concurrency control. To be set during a read-modify-write to ensure that the client has not attempted to modify a changed resource.  | [optional] 
**name** | **str** |  | 
**criteria** | **str** | Internal representation of the cohort definition. Clients should not depend directly on this, but instead call client functions to issue a SQL query for the cohort.  | 
**type** | **str** |  | 
**description** | **str** |  | [optional] 
**creator** | **str** |  | [optional] 
**creation_time** | **int** | Milliseconds since the UNIX epoch. | [optional] 
**last_modified_time** | **int** | Milliseconds since the UNIX epoch. | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


