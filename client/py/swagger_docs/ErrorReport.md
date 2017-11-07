# ErrorReport

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**source** | **str** | service causing error | 
**message** | **str** | what went wrong | 
**exception_class** | **str** | class of exception thrown | [optional] 
**status_code** | **int** | HTTP status code | [optional] 
**causes** | [**list[ErrorReport]**](ErrorReport.md) | errors triggering this one | 
**stack_trace** | [**list[StackTraceElement]**](StackTraceElement.md) | stack trace | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


