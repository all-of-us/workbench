# swagger_client.CohortsApi

All URIs are relative to *https://api.pmi-ops.org*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create_cohort**](CohortsApi.md#create_cohort) | **POST** /api/v1/workspaces/{workspaceNamespace}/{workspaceId}/cohorts | 
[**delete_cohort**](CohortsApi.md#delete_cohort) | **DELETE** /api/v1/workspaces/{workspaceNamespace}/{workspaceId}/cohorts/{cohortId} | 
[**get_cohort**](CohortsApi.md#get_cohort) | **GET** /api/v1/workspaces/{workspaceNamespace}/{workspaceId}/cohorts/{cohortId} | 
[**get_cohorts_in_workspace**](CohortsApi.md#get_cohorts_in_workspace) | **GET** /api/v1/workspaces/{workspaceNamespace}/{workspaceId}/cohorts | 
[**update_cohort**](CohortsApi.md#update_cohort) | **PATCH** /api/v1/workspaces/{workspaceNamespace}/{workspaceId}/cohorts/{cohortId} | 


# **create_cohort**
> Cohort create_cohort(workspace_namespace, workspace_id, cohort=cohort)



Creates a cohort definition in a workspace.

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
api_instance = swagger_client.CohortsApi(swagger_client.ApiClient(configuration))
workspace_namespace = 'workspace_namespace_example' # str | namespace containing the workspace to create a cohort definition in
workspace_id = 'workspace_id_example' # str | ID of the workspace to create a cohort definition in
cohort = swagger_client.Cohort() # Cohort | cohort definition (optional)

try: 
    api_response = api_instance.create_cohort(workspace_namespace, workspace_id, cohort=cohort)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling CohortsApi->create_cohort: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**| namespace containing the workspace to create a cohort definition in | 
 **workspace_id** | **str**| ID of the workspace to create a cohort definition in | 
 **cohort** | [**Cohort**](Cohort.md)| cohort definition | [optional] 

### Return type

[**Cohort**](Cohort.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **delete_cohort**
> delete_cohort(workspace_namespace, workspace_id, cohort_id)



Deletes the cohort definition with the specified ID

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
api_instance = swagger_client.CohortsApi(swagger_client.ApiClient(configuration))
workspace_namespace = 'workspace_namespace_example' # str | namespace of the workspace containing the cohort definition
workspace_id = 'workspace_id_example' # str | ID of the workspace containing the cohort definition
cohort_id = 'cohort_id_example' # str | ID of the workspace containing the cohort definition

try: 
    api_instance.delete_cohort(workspace_namespace, workspace_id, cohort_id)
except ApiException as e:
    print("Exception when calling CohortsApi->delete_cohort: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**| namespace of the workspace containing the cohort definition | 
 **workspace_id** | **str**| ID of the workspace containing the cohort definition | 
 **cohort_id** | **str**| ID of the workspace containing the cohort definition | 

### Return type

void (empty response body)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_cohort**
> Cohort get_cohort(workspace_namespace, workspace_id, cohort_id)



Returns the cohort definition with the specified ID

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
api_instance = swagger_client.CohortsApi(swagger_client.ApiClient(configuration))
workspace_namespace = 'workspace_namespace_example' # str | namespace of the workspace containing the cohort definition
workspace_id = 'workspace_id_example' # str | ID of the workspace containing the cohort definition
cohort_id = 'cohort_id_example' # str | ID of the workspace containing the cohort definition

try: 
    api_response = api_instance.get_cohort(workspace_namespace, workspace_id, cohort_id)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling CohortsApi->get_cohort: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**| namespace of the workspace containing the cohort definition | 
 **workspace_id** | **str**| ID of the workspace containing the cohort definition | 
 **cohort_id** | **str**| ID of the workspace containing the cohort definition | 

### Return type

[**Cohort**](Cohort.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_cohorts_in_workspace**
> CohortListResponse get_cohorts_in_workspace(workspace_namespace, workspace_id)



Returns all cohort definitions in a workspace

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
api_instance = swagger_client.CohortsApi(swagger_client.ApiClient(configuration))
workspace_namespace = 'workspace_namespace_example' # str | namespace containing the workspace to retrieve cohort definitions from
workspace_id = 'workspace_id_example' # str | ID of the workspace to retrieve cohort definitions from.

try: 
    api_response = api_instance.get_cohorts_in_workspace(workspace_namespace, workspace_id)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling CohortsApi->get_cohorts_in_workspace: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**| namespace containing the workspace to retrieve cohort definitions from | 
 **workspace_id** | **str**| ID of the workspace to retrieve cohort definitions from. | 

### Return type

[**CohortListResponse**](CohortListResponse.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **update_cohort**
> Cohort update_cohort(workspace_namespace, workspace_id, cohort_id, cohort=cohort)



Modifies the cohort definition with the specified ID; fields that are omitted will not be modified 

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
api_instance = swagger_client.CohortsApi(swagger_client.ApiClient(configuration))
workspace_namespace = 'workspace_namespace_example' # str | namespace of the workspace containing the cohort definition
workspace_id = 'workspace_id_example' # str | ID of the workspace containing the cohort definition
cohort_id = 'cohort_id_example' # str | ID of the workspace containing the cohort definition
cohort = swagger_client.Cohort() # Cohort | cohort definition (optional)

try: 
    api_response = api_instance.update_cohort(workspace_namespace, workspace_id, cohort_id, cohort=cohort)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling CohortsApi->update_cohort: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**| namespace of the workspace containing the cohort definition | 
 **workspace_id** | **str**| ID of the workspace containing the cohort definition | 
 **cohort_id** | **str**| ID of the workspace containing the cohort definition | 
 **cohort** | [**Cohort**](Cohort.md)| cohort definition | [optional] 

### Return type

[**Cohort**](Cohort.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

