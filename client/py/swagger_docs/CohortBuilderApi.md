# swagger_client.CohortBuilderApi

All URIs are relative to *https://api.pmi-ops.org*

Method | HTTP request | Description
------------- | ------------- | -------------
[**count_subjects**](CohortBuilderApi.md#count_subjects) | **POST** /api/v1/cohort-builder/search | 
[**get_criteria_by_type_and_parent_id**](CohortBuilderApi.md#get_criteria_by_type_and_parent_id) | **GET** /api/v1/cohort-builder/criteria/{type}/{parentId} | 


# **count_subjects**
> int count_subjects(request)



Searches for subjects based on criteria, criteria specific parameters, and modifiers.

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# Configure OAuth2 access token for authorization: aou_oauth
configuration = swagger_client.Configuration()
configuration.access_token = 'YOUR_ACCESS_TOKEN'

# create an instance of the API class
api_instance = swagger_client.CohortBuilderApi(swagger_client.ApiClient(configuration))
request = swagger_client.SearchRequest() # SearchRequest | object of parameters by which to perform the search

try: 
    api_response = api_instance.count_subjects(request)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling CohortBuilderApi->count_subjects: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **request** | [**SearchRequest**](SearchRequest.md)| object of parameters by which to perform the search | 

### Return type

**int**

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_criteria_by_type_and_parent_id**
> CriteriaListResponse get_criteria_by_type_and_parent_id(type, parent_id)



Returns builder criteria tree with the specified criteria type and parentId

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# Configure OAuth2 access token for authorization: aou_oauth
configuration = swagger_client.Configuration()
configuration.access_token = 'YOUR_ACCESS_TOKEN'

# create an instance of the API class
api_instance = swagger_client.CohortBuilderApi(swagger_client.ApiClient(configuration))
type = 'type_example' # str | the specific type of criteria to get
parent_id = 789 # int | fetch children of parentId

try: 
    api_response = api_instance.get_criteria_by_type_and_parent_id(type, parent_id)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling CohortBuilderApi->get_criteria_by_type_and_parent_id: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **type** | **str**| the specific type of criteria to get | 
 **parent_id** | **int**| fetch children of parentId | 

### Return type

[**CriteriaListResponse**](CriteriaListResponse.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

