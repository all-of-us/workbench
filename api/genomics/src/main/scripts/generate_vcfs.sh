#!/usr/bin/env bash

while getopts "n:v:" o; do
  case $o in
    n) NUM_VCFS=$OPTARG;;
    v) SEED_VCF=$OPTARG;;
  esac
done

for INDEX in $(eval echo {0..${NUM_VCFS}}); do
  OUTPUT_NAME=$(echo "${SEED_VCF}" | cut -f 1 -d '.')
  ./gradlew -p genomics randomizeVcf -PappArgs="['-V${SEED_VCF}','-O${OUTPUT_NAME}.${INDEX}.vcf','-S ${INDEX}']"
done
