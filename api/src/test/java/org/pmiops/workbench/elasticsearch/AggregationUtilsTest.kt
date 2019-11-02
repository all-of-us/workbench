package org.pmiops.workbench.elasticsearch

import com.google.common.truth.Truth.assertThat
import org.pmiops.workbench.elasticsearch.AggregationUtils.DATE
import org.pmiops.workbench.elasticsearch.AggregationUtils.GENDER
import org.pmiops.workbench.elasticsearch.AggregationUtils.RACE
import org.pmiops.workbench.elasticsearch.AggregationUtils.RANGE_19_44

import org.elasticsearch.search.aggregations.AggregationBuilder
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class AggregationUtilsTest {

    @Test
    @Throws(Exception::class)
    fun buildDemoChartAggregation() {
        val actualBuilder = AggregationUtils.buildDemoChartAggregation(RANGE_19_44)
        assertThat(actualBuilder.name).isEqualTo(DATE + RANGE_19_44)
        assertThat(actualBuilder.type).isEqualTo("date_range")
        assertThat(actualBuilder.subAggregations.size).isEqualTo(1)
        val genderBuilder = actualBuilder.subAggregations.iterator().next()
        assertThat(genderBuilder.name).isEqualTo(GENDER + RANGE_19_44)
        assertThat(genderBuilder.type).isEqualTo("terms")
        assertThat(genderBuilder.subAggregations.size).isEqualTo(1)
        val raceBuilder = genderBuilder.subAggregations.iterator().next()
        assertThat(raceBuilder.name).isEqualTo(RACE + RANGE_19_44)
        assertThat(raceBuilder.type).isEqualTo("terms")
        assertThat(raceBuilder.subAggregations.isEmpty()).isTrue()
    }
}
