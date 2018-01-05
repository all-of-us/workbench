# swagger_client.ConfigApi

All URIs are relative to *https://api.pmi-ops.org*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_config**](ConfigApi.md#get_config) | **GET** /api/v1/config | 


# **get_config**
> ConfigResponse get_config()



Returns some server configuration data.

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.ConfigApi()

try: 
    api_response = api_instance.get_config()
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ConfigApi->get_config: %s\n" % e)
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**ConfigResponse**](ConfigResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

