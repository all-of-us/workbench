package org.pmiops.workbench.elasticsearch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.range.ParsedDateRange;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.pmiops.workbench.model.DemoChartInfo;

public class AggregationUtils {

  public static final String RANGE_18_44 = "18-44";
  public static final String RANGE_45_64 = "45-64";
  public static final String RANGE_GT_65 = "65";
  public static final String DATE = "d_";
  public static final String GENDER = "g_";
  public static final String RACE = "r_";

  /**
   * Build aggregations for demographic charting based on date range, gender and race. The bucket
   * aggregations compute and return the number of documents that "fell into" each bucket.
   */
  public static AggregationBuilder buildDemoChartAggregation(String ageRange) {
    String[] ages = ageRange.split("-");
    LocalDate start = null;
    // use the low end of the age range to calculate the end of the date range
    LocalDate end = ElasticUtils.todayMinusYears(Integer.parseInt(ages[0]));
    if (ages.length > 1) {
      // use high end of the age range to calculate the start of the date range
      // need to add 1 year to adjust the start date to beginning of age range
      start = ElasticUtils.todayMinusYears(Integer.parseInt(ages[1]) + 1);
    }

    // Added order to gender and race buckets. Therefore the UI code can expect consistent results
    // between BQ(sql ordered by gender, race, age) and elastic
    return AggregationBuilders.dateRange(DATE + ageRange)
        .field("birth_datetime")
        .format("yyyy-MM-dd")
        .addRange((start == null) ? null : start.toString(), end.toString())
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
                        .minDocCount(1)));
  }

  public static List<DemoChartInfo> unwrapDemoChartBuckets(
      SearchResponse searchResponse, String... ageRanges) {
    List<DemoChartInfo> demoInformation = new ArrayList<>();
    for (String ageRange : ageRanges) {
      ParsedDateRange parsedDate = searchResponse.getAggregations().get(DATE + ageRange);
      for (Range.Bucket dateBucket : parsedDate.getBuckets()) {
        Terms gender = dateBucket.getAggregations().get(GENDER + ageRange);
        for (Terms.Bucket genderBucket : gender.getBuckets()) {
          Terms race = genderBucket.getAggregations().get(RACE + ageRange);
          for (Terms.Bucket raceBucket : race.getBuckets()) {
            demoInformation.add(
                new DemoChartInfo()
                    .gender(genderBucket.getKeyAsString().substring(0, 1))
                    .race(raceBucket.getKeyAsString())
                    .ageRange(RANGE_GT_65.equals(ageRange) ? "> " + ageRange : ageRange)
                    .count(raceBucket.getDocCount()));
          }
        }
      }
    }
    return demoInformation;
  }
}
