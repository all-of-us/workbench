# swagger_client.BugReportApi

All URIs are relative to *https://api.pmi-ops.org*

Method | HTTP request | Description
------------- | ------------- | -------------
[**send_bug_report**](BugReportApi.md#send_bug_report) | **POST** /api/v1/sendBugReport | 


# **send_bug_report**
> BugReport send_bug_report(bug_report)



Sends an email to developers about a user reported bug

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
api_instance = swagger_client.BugReportApi(swagger_client.ApiClient(configuration))
bug_report = swagger_client.BugReport() # BugReport | user reported bug

try: 
    api_response = api_instance.send_bug_report(bug_report)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling BugReportApi->send_bug_report: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **bug_report** | [**BugReport**](BugReport.md)| user reported bug | 

### Return type

[**BugReport**](BugReport.md)

### Authorization

[aou_oauth](../README.md#aou_oauth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

