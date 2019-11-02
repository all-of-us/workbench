package org.pmiops.workbench.elasticsearch

import java.time.LocalDate
import java.util.ArrayList
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.aggregations.AggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.BucketOrder
import org.elasticsearch.search.aggregations.bucket.range.ParsedDateRange
import org.elasticsearch.search.aggregations.bucket.range.Range
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.pmiops.workbench.model.DemoChartInfo

object AggregationUtils {

    val RANGE_19_44 = "19-44"
    val RANGE_45_64 = "45-64"
    val RANGE_GT_65 = "65"
    val DATE = "d_"
    val GENDER = "g_"
    val RACE = "r_"

    /**
     * Build aggregations for demographic charting based on date range, gender and race. The bucket
     * aggregations compute and return the number of documents that "fell into" each bucket.
     */
    fun buildDemoChartAggregation(ageRange: String): AggregationBuilder {
        val ages = ageRange.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var start: LocalDate? = null
        // use the low end of the age range to calculate the end of the date range
        val end = ElasticUtils.todayMinusYears(Integer.parseInt(ages[0]))
        if (ages.size > 1) {
            // use high end of the age range to calculate the start of the date range
            // need to add 1 year to adjust the start date to beginning of age range
            start = ElasticUtils.todayMinusYears(Integer.parseInt(ages[1]) + 1)
        }

        // Added order to gender and race buckets. Therefore the UI code can expect consistent results
        // between BQ(sql ordered by gender, race, age) and elastic
        return AggregationBuilders.dateRange(DATE + ageRange)
                .field("birth_datetime")
                .format("yyyy-MM-dd")
                .addRange(start?.toString(), end.toString())
                .subAggregation(
                        AggregationBuilders.terms(GENDER + ageRange)
                                .field("gender_concept_name")
                                .order(BucketOrder.key(true))
                                .subAggregation(
                                        AggregationBuilders.terms(RACE + ageRange)
                                                .field("race_concept_name")
                                                .order(BucketOrder.key(true))
                                                // This eliminates the race buckets with 0 counts. Without this param
                                                // elastic
                                                // returns all race buckets regardless of count.
                                                .minDocCount(1)))
    }

    fun unwrapDemoChartBuckets(
            searchResponse: SearchResponse, vararg ageRanges: String): List<DemoChartInfo> {
        val demoInformation = ArrayList<DemoChartInfo>()
        for (ageRange in ageRanges) {
            val parsedDate = searchResponse.aggregations.get<ParsedDateRange>(DATE + ageRange)
            for (dateBucket in parsedDate.buckets) {
                val gender = dateBucket.aggregations.get<Terms>(GENDER + ageRange)
                for (genderBucket in gender.buckets) {
                    val race = genderBucket.aggregations.get<Terms>(RACE + ageRange)
                    for (raceBucket in race.buckets) {
                        demoInformation.add(
                                DemoChartInfo()
                                        .gender(genderBucket.keyAsString.substring(0, 1))
                                        .race(raceBucket.keyAsString)
                                        .ageRange(if (RANGE_GT_65 == ageRange) "> $ageRange" else ageRange)
                                        .count(raceBucket.docCount))
                    }
                }
            }
        }
        return demoInformation
    }
}
