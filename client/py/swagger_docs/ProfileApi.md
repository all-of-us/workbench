# swagger_client.ProfileApi

All URIs are relative to *https://api.pmi-ops.org*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create_account**](ProfileApi.md#create_account) | **POST** /api/v1/google-account | 
[**delete_account**](ProfileApi.md#delete_account) | **DELETE** /api/v1/google-account | 
[**get_me**](ProfileApi.md#get_me) | **GET** /api/v1/me | 
[**is_username_taken**](ProfileApi.md#is_username_taken) | **GET** /api/v1/is-username-taken | 
[**register**](ProfileApi.md#register) | **POST** /api/v1/register | 


# **create_account**
> Profile create_account(create_account_request=create_account_request)



Creates an account in the researchallofus.org domain.

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.ProfileApi()
create_account_request = swagger_client.CreateAccountRequest() # CreateAccountRequest |  (optional)

try: 
    api_response = api_instance.create_account(create_account_request=create_account_request)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProfileApi->create_account: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **create_account_request** | [**CreateAccountRequest**](CreateAccountRequest.md)|  | [optional] 

### Return type

[**Profile**](Profile.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **delete_account**
> delete_account()



Deletes your account in the researchallofus.org domain.

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
    api_instance.delete_account()
except ApiException as e:
    print("Exception when calling ProfileApi->delete_account: %s\n" % e)
```

### Parameters
This endpoint does not need any parameter.

### Return type

void (empty response body)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

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

# **is_username_taken**
> UsernameTakenResponse is_username_taken(username)



Checks to see if the given username is not available.

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.ProfileApi()
username = 'username_example' # str | 

try: 
    api_response = api_instance.is_username_taken(username)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProfileApi->is_username_taken: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **username** | **str**|  | 

### Return type

[**UsernameTakenResponse**](UsernameTakenResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **register**
> Profile register(registration_request=registration_request)



Registers a profile for the user in AllOfUs and Firecloud; called after the user has consented to the terms of use

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

