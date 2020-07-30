# Synthetic Variant Generation

Run from `workbench/api`, the following command will generate a number of synthetic vcfs randomized from a vcf run on the AoU array and write them into a given directory:

```
./project.rb randomize-vcf --vcf [path to gzipped vcf file] --number-of-copies [n] --output-dir [wherever you want to put them]
```

This tool is based on a GATK VariantWalker; as such, it expects the input vcf to be gzipped, and it expects a vcf index file with the same basename to be in the same directory as the input vcf. A vcf of NA12878 run on the correct can be found at gs://all-of-us-workbench-test-genomics/NA12878_204126160130_R01C01.vcf.

Run from `workbench/api`, the following command will merge all vcfs in a directory into a single multisample vcf, assuming that all the vcfs were run on the same microarray, have different sample names, and have index files in the same directory with the same basenames:

```
./project.rb combine-vcfs --dir [path to directory with vcfs] --output [name of output vcf]
```
