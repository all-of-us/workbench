package org.pmiops.workbench.elasticsearch

import org.pmiops.workbench.elasticsearch.AggregationUtils.RANGE_19_44
import org.pmiops.workbench.elasticsearch.AggregationUtils.RANGE_45_64
import org.pmiops.workbench.elasticsearch.AggregationUtils.RANGE_GT_65
import org.pmiops.workbench.elasticsearch.AggregationUtils.buildDemoChartAggregation
import org.pmiops.workbench.elasticsearch.AggregationUtils.unwrapDemoChartBuckets

import java.io.IOException
import java.net.URL
import java.util.logging.Logger
import javax.inject.Provider
import jnr.ffi.annotations.Synchronized
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.core.CountRequest
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.json.JSONObject
import org.pmiops.workbench.cdr.CdrVersionContext
import org.pmiops.workbench.cdr.dao.CBCriteriaDao
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchConfig.ElasticsearchConfig
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.model.DemoChartInfo
import org.pmiops.workbench.model.SearchRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ElasticSearchService @Autowired
constructor(
        private val cbCriteriaDao: CBCriteriaDao,
        private val cloudStorageService: CloudStorageService,
        private val configProvider: Provider<WorkbenchConfig>) {
    private var client: RestHighLevelClient? = null

    /** Get the total participant count matching the given search criteria.  */
    @Throws(IOException::class)
    fun count(req: SearchRequest): Long? {
        val personIndex = ElasticUtils.personIndexName(CdrVersionContext.getCdrVersion().elasticIndexBaseName)
        val filter = ElasticFilters.fromCohortSearch(cbCriteriaDao, req)
        log.info("Elastic filter: $filter")
        return client()
                .count(
                        CountRequest(personIndex)
                                .source(SearchSourceBuilder.searchSource().query(filter)),
                        RequestOptions.DEFAULT)
                .count
    }

    /** Get the demographic data info for the given search criteria.  */
    @Throws(IOException::class)
    fun demoChartInfo(req: SearchRequest): List<DemoChartInfo> {
        val personIndex = ElasticUtils.personIndexName(CdrVersionContext.getCdrVersion().elasticIndexBaseName)
        val filter = ElasticFilters.fromCohortSearch(cbCriteriaDao, req)
        log.info("Elastic filter: $filter")
        val searchResponse = client()
                .search(
                        org.elasticsearch.action.search.SearchRequest(personIndex)
                                .source(
                                        SearchSourceBuilder.searchSource()
                                                .size(0) // reduce the payload since were only interested in the
                                                // aggregations
                                                .query(filter)
                                                .aggregation(buildDemoChartAggregation(RANGE_19_44))
                                                .aggregation(buildDemoChartAggregation(RANGE_45_64))
                                                .aggregation(buildDemoChartAggregation(RANGE_GT_65))),
                        RequestOptions.DEFAULT)
        return unwrapDemoChartBuckets(searchResponse, RANGE_19_44, RANGE_45_64, RANGE_GT_65)
    }

    /**
     * Implementing RestHighLevelClient init here because injecting Provider<WorkbenchConfig> into a
     * Configuration singleton class was causing a BeanInstantiationException due to WorkbenchConfig
     * being request scoped. This works but need to add Synchronized annotation to make this method
     * thread safe.
    </WorkbenchConfig> */
    @Synchronized
    @Throws(IOException::class)
    private fun client(): RestHighLevelClient {
        val esConfig = configProvider.get().elasticsearch
        if (client == null) {
            val url = URL(esConfig.baseUrl)
            val builder = RestClient.builder(HttpHost(url.host, url.port, url.protocol))
            if (esConfig.enableBasicAuth) {
                val creds = cloudStorageService.elasticCredentials
                val credentialsProvider = BasicCredentialsProvider()
                credentialsProvider.setCredentials(
                        AuthScope.ANY,
                        UsernamePasswordCredentials(
                                creds.getString("username"), creds.getString("password")))
                builder.setHttpClientConfigCallback { httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider) }
            }
            client = RestHighLevelClient(builder)
        }
        return client
    }

    companion object {

        private val log = Logger.getLogger(ElasticSearchService::class.java.name)
    }
}
