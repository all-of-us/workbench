package spark;

import com.google.common.collect.ImmutableList;
import htsjdk.variant.variantcontext.GenotypeBuilder;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.vcf.VCFHeader;
import java.util.Set;
import org.apache.spark.sql.SparkSession;
import org.broadinstitute.hellbender.engine.ProgressMeter;
import org.broadinstitute.hellbender.engine.ReferenceDataSource;
import org.broadinstitute.hellbender.tools.variantdb.CommonCode;
import org.broadinstitute.hellbender.tools.variantdb.nextgen.ExtractCohort;
import org.broadinstitute.hellbender.tools.walkers.annotator.VariantAnnotatorEngine;
import scala.collection.JavaConverters;
import scala.reflect.ClassTag$;

public class App {

  static int numSlices;
  static String projectID;
  static VariantContextWriter vcfWriter;
  static VCFHeader vcfHeader;
  static VariantAnnotatorEngine annotationEngine;
  static ReferenceDataSource refSource;
  static Set<String> sampleNames;
  static CommonCode.ModeEnum mode;
  static String cohortTableName;

  static String filteringTableName;
  static int localSortMaxRecordsInRam;
  static boolean printDebugInformation;
  static double vqsLodSNPThreshold;
  static double vqsLodINDELThreshold;
  static ProgressMeter progressMeter;
  static ExtractCohort.QueryMode queryMode;
  static String filterSetName;

  public static void main(String[] args) {
    SparkSession spark = SparkSession.builder().appName("Simple Application").getOrCreate();

    spark
        .sparkContext()
        .parallelize(
            JavaConverters.asScalaIterator(
                ImmutableList.of(new ExtractConfig(1L, 1000L, "gs://asdfasdf/foo")).iterator()),
            numSlices,
            ClassTag$.MODULE$.apply(ExtractConfig.class))
        .filter(
            config -> {
              System.out.println(config);
              return config.outputFilename;
            });
    System.out.println("XD");
    GenotypeBuilder genotypeBuilder = new GenotypeBuilder();
    System.out.println(genotypeBuilder);
    spark.stop();
  }
}
