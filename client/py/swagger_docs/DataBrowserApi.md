# swagger_client.DataBrowserApi

All URIs are relative to *https://api.pmi-ops.org*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_analyses**](DataBrowserApi.md#get_analyses) | **GET** /api/v1/databrowser/analyses | 
[**get_analysis_results**](DataBrowserApi.md#get_analysis_results) | **GET** /api/v1/databrowser/analysis-results | 
[**get_child_concepts**](DataBrowserApi.md#get_child_concepts) | **GET** /api/v1/databrowser/child-concepts | 
[**get_concept_count**](DataBrowserApi.md#get_concept_count) | **GET** /api/v1/databrowser/concept-count | 
[**get_concept_count_by_age**](DataBrowserApi.md#get_concept_count_by_age) | **GET** /api/v1/databrowser/concept-count-by-age | 
[**get_concept_count_by_gender**](DataBrowserApi.md#get_concept_count_by_gender) | **GET** /api/v1/databrowser/concept-count-by-gender | 
[**get_concepts_search**](DataBrowserApi.md#get_concepts_search) | **GET** /api/v1/databrowser/search-concepts | 
[**get_db_domains**](DataBrowserApi.md#get_db_domains) | **GET** /api/v1/databrowser/db-domains | 
[**get_parent_concepts**](DataBrowserApi.md#get_parent_concepts) | **GET** /api/v1/databrowser/parent-concepts | 
[**get_participant_count**](DataBrowserApi.md#get_participant_count) | **GET** /api/v1/databrowser/participant-count | 


# **get_analyses**
> AnalysisListResponse get_analyses()



Gets list of analysis definitions

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.DataBrowserApi()

try: 
    api_response = api_instance.get_analyses()
    pprint(api_response)
except ApiException as e:
    print("Exception when calling DataBrowserApi->get_analyses: %s\n" % e)
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AnalysisListResponse**](AnalysisListResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_analysis_results**
> AnalysisResultListResponse get_analysis_results(analysis_id, stratum_1=stratum_1, stratum_2=stratum_2)



Gets results for an analysis id and stratum

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.DataBrowserApi()
analysis_id = 789 # int | analysis_id
stratum_1 = 'stratum_1_example' # str | stratum 1 (optional)
stratum_2 = 'stratum_2_example' # str | stratum 2 (optional)

try: 
    api_response = api_instance.get_analysis_results(analysis_id, stratum_1=stratum_1, stratum_2=stratum_2)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling DataBrowserApi->get_analysis_results: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **analysis_id** | **int**| analysis_id | 
 **stratum_1** | **str**| stratum 1 | [optional] 
 **stratum_2** | **str**| stratum 2 | [optional] 

### Return type

[**AnalysisResultListResponse**](AnalysisResultListResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_child_concepts**
> ConceptListResponse get_child_concepts(concept_id)



Get children of the given concept

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.DataBrowserApi()
concept_id = 789 # int | concept id to get maps to concepts

try: 
    api_response = api_instance.get_child_concepts(concept_id)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling DataBrowserApi->get_child_concepts: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **concept_id** | **int**| concept id to get maps to concepts | 

### Return type

[**ConceptListResponse**](ConceptListResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_concept_count**
> AnalysisResultListResponse get_concept_count(concept_id)



Gets count for concept

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.DataBrowserApi()
concept_id = 'concept_id_example' # str | concept id to get count for

try: 
    api_response = api_instance.get_concept_count(concept_id)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling DataBrowserApi->get_concept_count: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **concept_id** | **str**| concept id to get count for | 

### Return type

[**AnalysisResultListResponse**](AnalysisResultListResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_concept_count_by_age**
> AnalysisResultListResponse get_concept_count_by_age(concept_id)



Gets counts for concept by age

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.DataBrowserApi()
concept_id = 'concept_id_example' # str | concept id to get count for

try: 
    api_response = api_instance.get_concept_count_by_age(concept_id)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling DataBrowserApi->get_concept_count_by_age: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **concept_id** | **str**| concept id to get count for | 

### Return type

[**AnalysisResultListResponse**](AnalysisResultListResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_concept_count_by_gender**
> AnalysisResultListResponse get_concept_count_by_gender(concept_id)



Gets counts for concept by gender

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.DataBrowserApi()
concept_id = 'concept_id_example' # str | concept id to get count for

try: 
    api_response = api_instance.get_concept_count_by_gender(concept_id)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling DataBrowserApi->get_concept_count_by_gender: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **concept_id** | **str**| concept id to get count for | 

### Return type

[**AnalysisResultListResponse**](AnalysisResultListResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_concepts_search**
> ConceptListResponse get_concepts_search(concept_name=concept_name, standard_concept=standard_concept, concept_code=concept_code, vocabulary_id=vocabulary_id, domain_id=domain_id)



Searches for concepts in concept table with search string and filters

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.DataBrowserApi()
concept_name = 'concept_name_example' # str | concept name search string (optional)
standard_concept = 'standard_concept_example' # str | standard concept filter (optional)
concept_code = 'concept_code_example' # str | concept code  filter (optional)
vocabulary_id = 'vocabulary_id_example' # str | vocabulary id filter (optional)
domain_id = 'domain_id_example' # str | vocabulary id filter (optional)

try: 
    api_response = api_instance.get_concepts_search(concept_name=concept_name, standard_concept=standard_concept, concept_code=concept_code, vocabulary_id=vocabulary_id, domain_id=domain_id)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling DataBrowserApi->get_concepts_search: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **concept_name** | **str**| concept name search string | [optional] 
 **standard_concept** | **str**| standard concept filter | [optional] 
 **concept_code** | **str**| concept code  filter | [optional] 
 **vocabulary_id** | **str**| vocabulary id filter | [optional] 
 **domain_id** | **str**| vocabulary id filter | [optional] 

### Return type

[**ConceptListResponse**](ConceptListResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_db_domains**
> DbDomainListResponse get_db_domains()



Gets list of analysis definitions

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.DataBrowserApi()

try: 
    api_response = api_instance.get_db_domains()
    pprint(api_response)
except ApiException as e:
    print("Exception when calling DataBrowserApi->get_db_domains: %s\n" % e)
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**DbDomainListResponse**](DbDomainListResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_parent_concepts**
> ConceptListResponse get_parent_concepts(concept_id)



Gets parent concepts for the given concept

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.DataBrowserApi()
concept_id = 789 # int | concept id to get maps to concepts

try: 
    api_response = api_instance.get_parent_concepts(concept_id)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling DataBrowserApi->get_parent_concepts: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **concept_id** | **int**| concept id to get maps to concepts | 

### Return type

[**ConceptListResponse**](ConceptListResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_participant_count**
> AnalysisResult get_participant_count()



Gets results for an analysis id and stratum

### Example 
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.DataBrowserApi()

try: 
    api_response = api_instance.get_participant_count()
    pprint(api_response)
except ApiException as e:
    print("Exception when calling DataBrowserApi->get_participant_count: %s\n" % e)
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AnalysisResult**](AnalysisResult.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

