import org.apache.spark.sql.SparkSession

object ExtractVcf {
  def main(args: Array[String]) {
    val spark = SparkSession.builder.appName("Extract VCFs").getOrCreate()
    println(s":O")
    spark.stop()
  }
}