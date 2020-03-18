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
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.GenderOrSexType;

public class AggregationUtils {

  public static final String RANGE_18_44 = "18-44";
  public static final String RANGE_45_64 = "45-64";
  public static final String RANGE_GT_65 = "65";
  public static final String DATE = "d_";
  public static final String GENDER_OR_SEX = "g_or_s_";
  public static final String RACE = "r_";

  /**
   * Build aggregations for demographic charting based on date range, gender and race. The bucket
   * aggregations compute and return the number of documents that "fell into" each bucket.
   */
  public static AggregationBuilder buildDemoChartAggregation(
      GenderOrSexType genderOrSexType, AgeType ageType, String ageRange) {
    String[] ages = ageRange.split("-");
    // use Integer values of age ranges when calculating age at consent or age at cdr
    Integer lo = Integer.valueOf(ages[0]);
    Integer hi = (ages.length > 1) ? Integer.valueOf(ages[1]) : null;

    // use high endDate of the age range to calculate the startDate of the date range
    // need to add 1 year to adjust the startDate date to beginning of age range
    LocalDate startDate = (hi != null) ? ElasticUtils.todayMinusYears(hi + 1) : null;
    // use the low endDate of the age range to calculate the endDate of the date range
    // when calculating age from date of birth
    LocalDate endDate = ElasticUtils.todayMinusYears(lo);

    boolean isGender = GenderOrSexType.GENDER.equals(genderOrSexType);
    boolean isAgeAtConsent = AgeType.AGE_AT_CONSENT.equals(ageType);
    // Added order to gender/sex and race buckets. Therefore the UI code can expect consistent
    // results between BQ(sql ordered by gender/sex, race, age) and elastic
    TermsAggregationBuilder termsAggregationBuilder =
        AggregationBuilders.terms(GENDER_OR_SEX + ageRange)
            .field(isGender ? "gender_concept_name" : "sex_at_birth_concept_name")
            .order(BucketOrder.key(true))
            .subAggregation(
                AggregationBuilders.terms(RACE + ageRange)
                    .field("race_concept_name")
                    .order(BucketOrder.key(true))
                    // This eliminates the race buckets with 0 counts. Without this param
                    // elastic returns all race buckets regardless of count.
                    .minDocCount(1));
    return AgeType.AGE.equals(ageType)
        ? AggregationBuilders.dateRange(DATE + ageRange)
            .field("birth_datetime")
            .format("yyyy-MM-dd")
            .addRange((startDate == null) ? null : startDate.toString(), endDate.toString())
            .subAggregation(termsAggregationBuilder)
        : AggregationBuilders.range(DATE + ageRange)
            .field(isAgeAtConsent ? "age_at_consent" : "age_at_cdr")
            .addRange(new Double(lo), new Double(hi))
            .subAggregation(termsAggregationBuilder);
  }

  public static List<DemoChartInfo> unwrapDemoChartBuckets(
      SearchResponse searchResponse, String... ageRanges) {
    List<DemoChartInfo> demoInformation = new ArrayList<>();
    for (String ageRange : ageRanges) {
      ParsedDateRange parsedDate = searchResponse.getAggregations().get(DATE + ageRange);
      for (Range.Bucket dateBucket : parsedDate.getBuckets()) {
        Terms gender = dateBucket.getAggregations().get(GENDER_OR_SEX + ageRange);
        for (Terms.Bucket genderBucket : gender.getBuckets()) {
          Terms race = genderBucket.getAggregations().get(RACE + ageRange);
          for (Terms.Bucket raceBucket : race.getBuckets()) {
            demoInformation.add(
                new DemoChartInfo()
                    .name(genderBucket.getKeyAsString().substring(0, 1))
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
