# swagger_client.WorkspacesApi

All URIs are relative to *https://api.pmi-ops.org*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create_workspace**](WorkspacesApi.md#create_workspace) | **POST** /api/v1/workspaces | 
[**delete_workspace**](WorkspacesApi.md#delete_workspace) | **DELETE** /api/v1/workspaces/{workspaceNamespace}/{workspaceId} | 
[**get_workspace**](WorkspacesApi.md#get_workspace) | **GET** /api/v1/workspaces/{workspaceNamespace}/{workspaceId} | 
[**get_workspaces**](WorkspacesApi.md#get_workspaces) | **GET** /api/v1/workspaces | 
[**update_workspace**](WorkspacesApi.md#update_workspace) | **PATCH** /api/v1/workspaces/{workspaceNamespace}/{workspaceId} | 


# **create_workspace**
> Workspace create_workspace(workspace=workspace)



Creates a workspace

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
api_instance = swagger_client.WorkspacesApi(swagger_client.ApiClient(configuration))
workspace = swagger_client.Workspace() # Workspace | workspace definition (optional)

try: 
    api_response = api_instance.create_workspace(workspace=workspace)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling WorkspacesApi->create_workspace: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace** | [**Workspace**](Workspace.md)| workspace definition | [optional] 

### Return type

[**Workspace**](Workspace.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **delete_workspace**
> delete_workspace(workspace_namespace, workspace_id)



Deletes the workspace definition with the specified ID and namespace

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
api_instance = swagger_client.WorkspacesApi(swagger_client.ApiClient(configuration))
workspace_namespace = 'workspace_namespace_example' # str | namespace of the workspace containing the cohort definition
workspace_id = 'workspace_id_example' # str | ID of the workspace containing the cohort definition

try: 
    api_instance.delete_workspace(workspace_namespace, workspace_id)
except ApiException as e:
    print("Exception when calling WorkspacesApi->delete_workspace: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**| namespace of the workspace containing the cohort definition | 
 **workspace_id** | **str**| ID of the workspace containing the cohort definition | 

### Return type

void (empty response body)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_workspace**
> Workspace get_workspace(workspace_namespace, workspace_id)



Returns the workspace definition with the specified ID and namespace

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
api_instance = swagger_client.WorkspacesApi(swagger_client.ApiClient(configuration))
workspace_namespace = 'workspace_namespace_example' # str | namespace of the workspace containing the cohort definition
workspace_id = 'workspace_id_example' # str | ID of the workspace containing the cohort definition

try: 
    api_response = api_instance.get_workspace(workspace_namespace, workspace_id)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling WorkspacesApi->get_workspace: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**| namespace of the workspace containing the cohort definition | 
 **workspace_id** | **str**| ID of the workspace containing the cohort definition | 

### Return type

[**Workspace**](Workspace.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_workspaces**
> WorkspaceListResponse get_workspaces()



Returns all workspaces that a user has access to

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
api_instance = swagger_client.WorkspacesApi(swagger_client.ApiClient(configuration))

try: 
    api_response = api_instance.get_workspaces()
    pprint(api_response)
except ApiException as e:
    print("Exception when calling WorkspacesApi->get_workspaces: %s\n" % e)
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**WorkspaceListResponse**](WorkspaceListResponse.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **update_workspace**
> Workspace update_workspace(workspace_namespace, workspace_id, workspace=workspace)



Modifies the workspace definition with the specified ID and namespace; fields that are omitted will not be modified 

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
api_instance = swagger_client.WorkspacesApi(swagger_client.ApiClient(configuration))
workspace_namespace = 'workspace_namespace_example' # str | namespace of the workspace containing the cohort definition
workspace_id = 'workspace_id_example' # str | ID of the workspace containing the cohort definition
workspace = swagger_client.Workspace() # Workspace | workspace definition (optional)

try: 
    api_response = api_instance.update_workspace(workspace_namespace, workspace_id, workspace=workspace)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling WorkspacesApi->update_workspace: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**| namespace of the workspace containing the cohort definition | 
 **workspace_id** | **str**| ID of the workspace containing the cohort definition | 
 **workspace** | [**Workspace**](Workspace.md)| workspace definition | [optional] 

### Return type

[**Workspace**](Workspace.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

