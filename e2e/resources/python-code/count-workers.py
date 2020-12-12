from pyspark import SparkContext
sc = SparkContext.getOrCreate()
sc.getConf().get("spark.executor.instances")
