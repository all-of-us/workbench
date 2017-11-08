# swagger_client.ClusterApi

All URIs are relative to *https://api.pmi-ops.org*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create_cluster**](ClusterApi.md#create_cluster) | **PUT** /api/v1/workspaces/{workspaceNamespace}/{workspaceId}/cluster/ | Creates a new dataproc cluster in the given project with the given name
[**delete_cluster**](ClusterApi.md#delete_cluster) | **DELETE** /api/v1/workspaces/{workspaceNamespace}/{workspaceId}/cluster/ | Deletes an existing dataproc cluster in the given project
[**get_cluster**](ClusterApi.md#get_cluster) | **GET** /api/v1/workspaces/{workspaceNamespace}/{workspaceId}/cluster/ | Get details of a dataproc cluster
[**list_clusters**](ClusterApi.md#list_clusters) | **GET** /api/v1/clusters | List all clusters


# **create_cluster**
> Cluster create_cluster(workspace_namespace, workspace_id)

Creates a new dataproc cluster in the given project with the given name

creates dataproc cluster with jupyters

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
api_instance = swagger_client.ClusterApi(swagger_client.ApiClient(configuration))
workspace_namespace = 'workspace_namespace_example' # str | workspaceNamespace
workspace_id = 'workspace_id_example' # str | workspaceId

try: 
    # Creates a new dataproc cluster in the given project with the given name
    api_response = api_instance.create_cluster(workspace_namespace, workspace_id)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ClusterApi->create_cluster: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**| workspaceNamespace | 
 **workspace_id** | **str**| workspaceId | 

### Return type

[**Cluster**](Cluster.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **delete_cluster**
> EmptyResponse delete_cluster(workspace_namespace, workspace_id)

Deletes an existing dataproc cluster in the given project

deletes a dataproc cluster

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
api_instance = swagger_client.ClusterApi(swagger_client.ApiClient(configuration))
workspace_namespace = 'workspace_namespace_example' # str | workspaceNamespace
workspace_id = 'workspace_id_example' # str | workspaceId

try: 
    # Deletes an existing dataproc cluster in the given project
    api_response = api_instance.delete_cluster(workspace_namespace, workspace_id)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ClusterApi->delete_cluster: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**| workspaceNamespace | 
 **workspace_id** | **str**| workspaceId | 

### Return type

[**EmptyResponse**](EmptyResponse.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_cluster**
> Cluster get_cluster(workspace_namespace, workspace_id)

Get details of a dataproc cluster

Returns information about an existing dataproc cluster managed by Leo. Poll this to find out when your cluster has finished starting up.

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
api_instance = swagger_client.ClusterApi(swagger_client.ApiClient(configuration))
workspace_namespace = 'workspace_namespace_example' # str | workspaceNamespace
workspace_id = 'workspace_id_example' # str | workspaceId

try: 
    # Get details of a dataproc cluster
    api_response = api_instance.get_cluster(workspace_namespace, workspace_id)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ClusterApi->get_cluster: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workspace_namespace** | **str**| workspaceNamespace | 
 **workspace_id** | **str**| workspaceId | 

### Return type

[**Cluster**](Cluster.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **list_clusters**
> ClusterListResponse list_clusters(labels=labels)

List all clusters

List all clusters, optionally filtering on a set of labels

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
api_instance = swagger_client.ClusterApi(swagger_client.ApiClient(configuration))
labels = 'labels_example' # str | Optional label key-value pairs to filter results by. Example: key1=val1,key2=val2.  Note: this string format is a workaround because Swagger doesn't support free-form query string parameters. The recommended way to use this endpoint is to specify the labels as top-level query string parameters. For instance: GET /api/clusters?key1=val1&key2=val2.  (optional)

try: 
    # List all clusters
    api_response = api_instance.list_clusters(labels=labels)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ClusterApi->list_clusters: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **labels** | **str**| Optional label key-value pairs to filter results by. Example: key1&#x3D;val1,key2&#x3D;val2.  Note: this string format is a workaround because Swagger doesn&#39;t support free-form query string parameters. The recommended way to use this endpoint is to specify the labels as top-level query string parameters. For instance: GET /api/clusters?key1&#x3D;val1&amp;key2&#x3D;val2.  | [optional] 

### Return type

[**ClusterListResponse**](ClusterListResponse.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

