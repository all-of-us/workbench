#!/usr/bin/env bash

while getopts "d:io:" o; do
  case $o in
    d) DIR=$OPTARG;;
    o) OUTPUT=$OPTARG;;
  esac
done

FILES="${DIR}*"
COMMAND="./gradlew -p genomics combineVCFs -PappArgs=\"["
for FILE in $FILES; do
  if [[ $FILE =~ \.vcf$ ]]; then
    COMMAND="${COMMAND}'I=${FILE}',"
  fi
done
COMMAND="${COMMAND}'O=${OUTPUT}']\""
eval $COMMAND

