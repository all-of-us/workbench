# SearchParameter

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**parameter_id** | **str** | Unique within the cohort definition | 
**name** | **str** | The name of the generating Criterion | 
**value** | **str** | The Value of the Criteria within its Category Tree | 
**type** | **str** | The type of the generating Criterion | 
**subtype** | **str** | The subtype of the generating Criterion | 
**group** | **bool** | Specifies if the generating Criterion is a leaf or node in its Criteria tree | 
**domain** | **str** | clue to determine which tables to search | [optional] 
**concept_id** | **int** | The concept id that maps to concept table. | [optional] 
**attribute** | [**Attribute**](Attribute.md) | Any applicable arguments that complete the sense of the parameter | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


