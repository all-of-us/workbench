# swagger_client.WorkspacesApi

All URIs are relative to *https://api.pmi-ops.org*

Method | HTTP request | Description
------------- | ------------- | -------------
[**clone_workspace**](WorkspacesApi.md#clone_workspace) | **POST** /api/v1/workspaces/{workspaceNamespace}/{workspaceId}/clone | 
[**create_workspace**](WorkspacesApi.md#create_workspace) | **POST** /api/v1/workspaces | 
[**delete_workspace**](WorkspacesApi.md#delete_workspace) | **DELETE** /api/v1/workspaces/{workspaceNamespace}/{workspaceId} | 
[**get_workspace**](WorkspacesApi.md#get_workspace) | **GET** /api/v1/workspaces/{workspaceNamespace}/{workspaceId} | 
[**get_workspaces**](WorkspacesApi.md#get_workspaces) | **GET** /api/v1/workspaces | 
[**get_workspaces_for_review**](WorkspacesApi.md#get_workspaces_for_review) | **GET** /api/v1/workspaces/review | 
[**review_workspace**](WorkspacesApi.md#review_workspace) | **POST** /api/v1/workspaces/{workspaceNamespace}/{workspaceId}/review | 
[**share_workspace**](WorkspacesApi.md#share_workspace) | **POST** /api/v1/workspaces/{workspaceNamespace}/{workspaceId}/share | 
[**update_workspace**](WorkspacesApi.md#update_workspace) | **PATCH** /api/v1/workspaces/{workspaceNamespace}/{workspaceId} | 


# **clone_workspace**
> CloneWorkspaceResponse clone_workspace(workspace_namespace, workspace_id, body=body)



Clone an existing workspace, with given modifications to workspace metadata. Caller will own the newly cloned workspace, and must have read access to the source workspace. In addition to workspace metadata, the following will also be cloned:   - the associated Firecloud workspace   - cohorts, along with reviews and annotations   - notebooks located in the default notebook directory for this workspace 

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
workspace_namespace = 'workspace_namespace_example' # str | 
workspace_id = 'workspace_id_example' # str | 
body = swagger_client.CloneWorkspaceRequest() # CloneWorkspaceRequest |  (optional)

try: 
    api_response = api_instance.clone_workspace(workspace_namespace, workspace_id, body=body)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling WorkspacesApi->clone_workspace: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**|  | 
 **workspace_id** | **str**|  | 
 **body** | [**CloneWorkspaceRequest**](CloneWorkspaceRequest.md)|  | [optional] 

### Return type

[**CloneWorkspaceResponse**](CloneWorkspaceResponse.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

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
> EmptyResponse delete_workspace(workspace_namespace, workspace_id)



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
    api_response = api_instance.delete_workspace(workspace_namespace, workspace_id)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling WorkspacesApi->delete_workspace: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**| namespace of the workspace containing the cohort definition | 
 **workspace_id** | **str**| ID of the workspace containing the cohort definition | 

### Return type

[**EmptyResponse**](EmptyResponse.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_workspace**
> WorkspaceResponse get_workspace(workspace_namespace, workspace_id)



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

[**WorkspaceResponse**](WorkspaceResponse.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_workspaces**
> WorkspaceResponseListResponse get_workspaces()



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

[**WorkspaceResponseListResponse**](WorkspaceResponseListResponse.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_workspaces_for_review**
> WorkspaceListResponse get_workspaces_for_review()



Returns workspaces that need research purpose review. Requires REVIEW_RESEARCH_PURPOSE authority. 

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
    api_response = api_instance.get_workspaces_for_review()
    pprint(api_response)
except ApiException as e:
    print("Exception when calling WorkspacesApi->get_workspaces_for_review: %s\n" % e)
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

# **review_workspace**
> EmptyResponse review_workspace(workspace_namespace, workspace_id, review=review)



Sets a research purpose review result.

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
workspace_namespace = 'workspace_namespace_example' # str | 
workspace_id = 'workspace_id_example' # str | 
review = swagger_client.ResearchPurposeReviewRequest() # ResearchPurposeReviewRequest | result of the research purpose review (optional)

try: 
    api_response = api_instance.review_workspace(workspace_namespace, workspace_id, review=review)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling WorkspacesApi->review_workspace: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**|  | 
 **workspace_id** | **str**|  | 
 **review** | [**ResearchPurposeReviewRequest**](ResearchPurposeReviewRequest.md)| result of the research purpose review | [optional] 

### Return type

[**EmptyResponse**](EmptyResponse.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **share_workspace**
> ShareWorkspaceResponse share_workspace(workspace_namespace, workspace_id, body=body)



Shares a workspace with users

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
workspace_namespace = 'workspace_namespace_example' # str | 
workspace_id = 'workspace_id_example' # str | 
body = swagger_client.ShareWorkspaceRequest() # ShareWorkspaceRequest | users to share the workspace with (optional)

try: 
    api_response = api_instance.share_workspace(workspace_namespace, workspace_id, body=body)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling WorkspacesApi->share_workspace: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**|  | 
 **workspace_id** | **str**|  | 
 **body** | [**ShareWorkspaceRequest**](ShareWorkspaceRequest.md)| users to share the workspace with | [optional] 

### Return type

[**ShareWorkspaceResponse**](ShareWorkspaceResponse.md)

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

