# swagger_client.ProfileApi

All URIs are relative to *https://api.pmi-ops.org*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_me**](ProfileApi.md#get_me) | **GET** /api/v1/me | 
[**register**](ProfileApi.md#register) | **POST** /api/v1/register | 


# **get_me**
> Profile get_me()



Returns the user's profile information

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
api_instance = swagger_client.ProfileApi(swagger_client.ApiClient(configuration))

try: 
    api_response = api_instance.get_me()
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProfileApi->get_me: %s\n" % e)
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**Profile**](Profile.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **register**
> Profile register(registration_request=registration_request)



Registers a profile for the user in AllOfUs and Firecloud

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
api_instance = swagger_client.ProfileApi(swagger_client.ApiClient(configuration))
registration_request = swagger_client.RegistrationRequest() # RegistrationRequest | registration request (optional)

try: 
    api_response = api_instance.register(registration_request=registration_request)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProfileApi->register: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **registration_request** | [**RegistrationRequest**](RegistrationRequest.md)| registration request | [optional] 

### Return type

[**Profile**](Profile.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

