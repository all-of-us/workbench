#!/usr/bin/env bash

# java -jar picard-2.23.2-3-ga5821b2-SNAPSHOT-all.jar CombineGenotypingArrayVcfs
# I=~/Documents/dev/misc/synthetic_vcf_testing/NA12878_204126160130_R01C01.0.vcf
# I=~/Documents/dev/misc/synthetic_vcf_testing/NA12878_204126160130_R01C01.1.vcf
# I=~/Documents/dev/misc/synthetic_vcf_testing/NA12878_204126160130_R01C01.2.vcf
# I=~/Documents/dev/misc/synthetic_vcf_testing/NA12878_204126160130_R01C01.3.vcf
# I=~/Documents/dev/misc/synthetic_vcf_testing/NA12878_204126160130_R01C01.4.vcf
# I=~/Documents/dev/misc/synthetic_vcf_testing/NA12878_204126160130_R01C01.5.vcf
# I=~/Documents/dev/misc/synthetic_vcf_testing/NA12878_204126160130_R01C01.6.vcf
# I=~/Documents/dev/misc/synthetic_vcf_testing/NA12878_204126160130_R01C01.7.vcf
# I=~/Documents/dev/misc/synthetic_vcf_testing/NA12878_204126160130_R01C01.8.vcf
# I=~/Documents/dev/misc/synthetic_vcf_testing/NA12878_204126160130_R01C01.9.vcf
# O=~/Documents/dev/misc/synthetic_vcf_testing/combined.vcf


while getopts "d:io:" o; do
  case $o in
    d) DIR=$OPTARG;;
    o) OUTPUT=$OPTARG;;
  esac
done

FILES="${DIR}*"
VCF_MATCHER="\.vcf[[:\>:]]"
COMMAND=
for FILE in FILES; do
  if [[ $FILE =~ $VCF_MATCHER ]]; then

  fi
done
