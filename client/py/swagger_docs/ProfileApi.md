# swagger_client.ProfileApi

All URIs are relative to *https://api.pmi-ops.org*

Method | HTTP request | Description
------------- | ------------- | -------------
[**complete_ethics_training**](ProfileApi.md#complete_ethics_training) | **POST** /api/v1/account/complete-ethics-training | 
[**create_account**](ProfileApi.md#create_account) | **POST** /api/v1/google-account | 
[**delete_account**](ProfileApi.md#delete_account) | **DELETE** /api/v1/google-account | 
[**get_me**](ProfileApi.md#get_me) | **GET** /api/v1/me | 
[**invitation_key_verification**](ProfileApi.md#invitation_key_verification) | **POST** /api/v1/invitation-key-verification | 
[**is_username_taken**](ProfileApi.md#is_username_taken) | **GET** /api/v1/is-username-taken | 
[**submit_demographics_survey**](ProfileApi.md#submit_demographics_survey) | **POST** /api/v1/account/submit-demographic-survey | 
[**submit_id_verification**](ProfileApi.md#submit_id_verification) | **POST** /api/v1/id-verification | 
[**submit_terms_of_service**](ProfileApi.md#submit_terms_of_service) | **POST** /api/v1/account/accept-terms-of-service | 
[**update_profile**](ProfileApi.md#update_profile) | **POST** /api/v1/update-profile | 


# **complete_ethics_training**
> Profile complete_ethics_training()



Completes ethics training.

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
    api_response = api_instance.complete_ethics_training()
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProfileApi->complete_ethics_training: %s\n" % e)
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

# **invitation_key_verification**
> invitation_key_verification(invitation_verification_request=invitation_verification_request)



Verifies invitation key.

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.ProfileApi()
invitation_verification_request = swagger_client.InvitationVerificationRequest() # InvitationVerificationRequest |  (optional)

try: 
    api_instance.invitation_key_verification(invitation_verification_request=invitation_verification_request)
except ApiException as e:
    print("Exception when calling ProfileApi->invitation_key_verification: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **invitation_verification_request** | [**InvitationVerificationRequest**](InvitationVerificationRequest.md)|  | [optional] 

### Return type

void (empty response body)

### Authorization

No authorization required

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

# **submit_demographics_survey**
> Profile submit_demographics_survey()



Submits demographic survey responses.

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
    api_response = api_instance.submit_demographics_survey()
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProfileApi->submit_demographics_survey: %s\n" % e)
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

# **submit_id_verification**
> Profile submit_id_verification(id_verification_request=id_verification_request)



Accepts identity information for verification.

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
id_verification_request = swagger_client.IdVerificationRequest() # IdVerificationRequest |  (optional)

try: 
    api_response = api_instance.submit_id_verification(id_verification_request=id_verification_request)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProfileApi->submit_id_verification: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id_verification_request** | [**IdVerificationRequest**](IdVerificationRequest.md)|  | [optional] 

### Return type

[**Profile**](Profile.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **submit_terms_of_service**
> Profile submit_terms_of_service()



Submits consent to the terms of service for researchers.

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
    api_response = api_instance.submit_terms_of_service()
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProfileApi->submit_terms_of_service: %s\n" % e)
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

# **update_profile**
> update_profile(updated_profile=updated_profile)



Updates a users profile

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
updated_profile = swagger_client.Profile() # Profile | the new profile to use (optional)

try: 
    api_instance.update_profile(updated_profile=updated_profile)
except ApiException as e:
    print("Exception when calling ProfileApi->update_profile: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **updated_profile** | [**Profile**](Profile.md)| the new profile to use | [optional] 

### Return type

void (empty response body)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

