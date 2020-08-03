# Synthetic Variant Generation

Run from `workbench/api`, the following command will generate a synthetic, multisample vcf randomized from a vcf run on the AoU array and write them into a given directory:

```
./project.rb randomize-vcf --vcf [path to gzipped vcf file] --number-of-samples [n] --output-dir [wherever you want to put them]
```

This tool is based on a GATK VariantWalker; as such, it expects the input vcf to be gzipped, and it expects a vcf index file with the same basename to be in the same directory as the input vcf. A vcf of NA12878 run on the correct can be found at gs://all-of-us-workbench-test-genomics/NA12878_204126160130_R01C01.vcf.
