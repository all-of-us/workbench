# Criteria

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **int** | id of the criteria | 
**type** | **str** | type of criteria | 
**subtype** | **str** | subtype of this criteria | [optional] 
**code** | **str** | code that identifies this criteria, ex: icd9, icd10 or cpt all have codes that map to the concept_code column in the concept table.  | 
**name** | **str** | description of criteria | 
**count** | **int** | est. count in the cdr | [optional] 
**group** | **bool** | specifies if child or parent | 
**selectable** | **bool** | specifies if user can search with | 
**concept_id** | **int** | concept ids are ids associated with a specific vocabulary item, in the concept table and allow you to search for that code in its domain table.  | [optional] 
**domain_id** | **str** | clue to determine which tables to search, but these only exist for leaves in the tree. Parents don&#39;t have domain ids and concept id will be used in the case that a parent is selectable.  | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


