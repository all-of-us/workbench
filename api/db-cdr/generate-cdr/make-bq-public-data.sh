#!/bin/bash

# This requires the raw count data in the public bq dataset which would be binned in place
# to sanitize the data for public consumption

set -xeuo pipefail
IFS=$'\n\t'

# --cdr=cdr_version ... *optional
USAGE="./generate-clousql-cdr/make-bq-public-data.sh --public-project <PROJECT> --public-dataset <DATASET> --bin-size <INT>"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --public-project) PUBLIC_PROJECT=$2; shift 2;;
    --public-dataset) PUBLIC_DATASET=$2; shift 2;;
    --bin-size) BIN_SIZE=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ -z "${PUBLIC_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${PUBLIC_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BIN_SIZE}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

# Check that source dataset exists and exit if not
datasets=$(bq --project=$PUBLIC_PROJECT ls)
if [ -z "$datasets" ]
then
  echo "$PUBLIC_PROJECT.$PUBLIC_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi
re=\\b$PUBLIC_DATASET\\b
if [[ $datasets =~ $re ]]; then
  echo "$PUBLIC_PROJECT.$PUBLIC_DATASET exists. Good. Carrying on."
else
  echo "$PUBLIC_PROJECT.$PUBLIC_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi

tables=$(bq --project=$PUBLIC_PROJECT --dataset=$PUBLIC_DATASET ls)
c_re=\\bconcept\\b
cr_re=\\bconcept_relationship\\b
ar_re=\\bachilles_results\\b
cri_re=\\bcriteria\\b
sm_re=\\bsurvey_module\\b
di_re=\\bdomain_info\\b
if [[ "$tables" =~ $c_re ]] && [[ "$tables" =~ $ar_re ]] && [[ "$tables" =~ $cr_re ]] && [[ "$tables" =~ $cri_re ]] && [[ "$tables" =~ $sm_re ]] && [[ "$tables" =~ $di_re ]]; then
    echo "Raw count tables exist. Carrying on."
else
    echo "Raw count tables does not exist. Please try generating counts on this dataset before proceeding."
    exit 1
fi

# Round counts for public dataset (The counts are rounded up using ceil. For example 4 to 20, 21 to 40, 43 to 60)
# 1. Set any count > 0 and < BIN_SIZE  to BIN_SIZE,
# 2. Round any above BIN_SIZE to multiple of BIN_SIZE

# Get person count to set prevalence
q="select count_value from \`${PUBLIC_PROJECT}.${PUBLIC_DATASET}.achilles_results\` a where a.analysis_id = 1"
person_count=$(bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql "$q" |  tr -dc '0-9')

# achilles_results
bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql \
"Update  \`$PUBLIC_PROJECT.$PUBLIC_DATASET.achilles_results\`
set count_value =
    case when count_value < ${BIN_SIZE}
        then ${BIN_SIZE}
    else
        cast(CEIL(count_value / ${BIN_SIZE}) * ${BIN_SIZE} as int64)
    end,
    source_count_value =
    case when source_count_value < ${BIN_SIZE} and source_count_value > 0
        then ${BIN_SIZE}
    else
        cast(CEIL(source_count_value / ${BIN_SIZE}) * ${BIN_SIZE} as int64)
    end
where count_value > 0"


#delete concepts with 0 count / source count value

bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql \
"delete from \`$PUBLIC_PROJECT.$PUBLIC_DATASET.concept\`
where (count_value=0 and source_count_value=0) and domain_id not in ('Race','Gender','Ethnicity','Unit','Drug')
and concept_code not in ('OMOP generated') and lower(vocabulary_id) not in ('ppi')"

#delete concepts from concept_relationship that are not in concepts
bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql \
"delete from \`$PUBLIC_PROJECT.$PUBLIC_DATASET.concept_relationship\`
where (concept_id_1 not in (select concept_id from \`$PUBLIC_PROJECT.$PUBLIC_DATASET.concept\`)) or (concept_id_2 not in (select concept_id from \`$PUBLIC_PROJECT.$PUBLIC_DATASET.concept\`))"

# concept bin size :
#Aggregate bin size will be set at 20. Counts lower than 20 will be displayed as 20; Counts higher than 20 will be rounded up to the closest multiple of 20. Eg: A count of 1245 will be displayed as 1260 .
bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql \
"Update  \`$PUBLIC_PROJECT.$PUBLIC_DATASET.concept\`
set count_value =
    case when count_value < ${BIN_SIZE}
        then ${BIN_SIZE}
    else
        cast(CEIL(count_value / ${BIN_SIZE}) * ${BIN_SIZE} as int64)
    end,
    source_count_value =
    case when source_count_value < ${BIN_SIZE} and source_count_value > 0
        then ${BIN_SIZE}
    else
        cast(CEIL(source_count_value / ${BIN_SIZE}) * ${BIN_SIZE} as int64)
    end,
    prevalence =
    case when count_value  > 0 and count_value < ${BIN_SIZE}
            then ROUND(CEIL(${BIN_SIZE} / ${person_count}),2)
        when count_value  > 0 and count_value >= ${BIN_SIZE}
            then ROUND(CEIL(CEIL(count_value / ${BIN_SIZE}) * ${BIN_SIZE}/ ${person_count}), 2)
        when source_count_value  > 0 and source_count_value < ${BIN_SIZE}
            then ROUND(CEIL(${BIN_SIZE} / ${person_count}),2)
        when source_count_value  > 0 and source_count_value >= ${BIN_SIZE}
            then ROUND(CEIL(CEIL(source_count_value / ${BIN_SIZE}) * ${BIN_SIZE}/ ${person_count}), 2)
        else
            0.00
    end
where count_value > 0 or lower(vocabulary_id) ='ppi' "

# criteria
bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql \
"Update  \`$PUBLIC_PROJECT.$PUBLIC_DATASET.criteria\`
set est_count =
    case when est_count < ${BIN_SIZE}
        then ${BIN_SIZE}
    else
        cast(ROUND(est_count / ${BIN_SIZE}) * ${BIN_SIZE} as int64)
    end
where est_count > 0"

# domain_info
bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql \
"Update  \`$PUBLIC_PROJECT.$PUBLIC_DATASET.domain_info\`
set participant_count =
    case when participant_count < ${BIN_SIZE}
        then ${BIN_SIZE}
    else
        cast(CEIL(participant_count / ${BIN_SIZE}) * ${BIN_SIZE} as int64)
    end
where participant_count > 0"

# survey_module
bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql \
"Update  \`$PUBLIC_PROJECT.$PUBLIC_DATASET.survey_module\`
set participant_count =
    case when participant_count < ${BIN_SIZE}
        then ${BIN_SIZE}
    else
        cast(CEIL(participant_count / ${BIN_SIZE}) * ${BIN_SIZE} as int64)
    end
where participant_count > 0"

# Updating domain_id of few survey questions from measurement to observation to avoid confusion in display
bq --quiet --project=$PUBLIC_PROJECT query --nouse_legacy_sql \
"Update  \`$PUBLIC_PROJECT.$PUBLIC_DATASET.concept\`
set domain_id = 'Observation'
where concept_id in (40770349, 40766240, 40766930, 40766219, 40767339, 40766306, 40766645, 40766357, 40769140, 40766229, 40767407, 40766333, 40766241, 40766929, 40766643, 40766307)"
