#!/bin/bash

# This generates big query count databases cdr and public that get put in cloudsql for workbench and data browser

set -xeuo pipefail
IFS=$'\n\t'


# get options
# --project=all-of-us-workbench-test *required

# --cdr=cdr_version ... *optional
USAGE="./generate-clousql-cdr/make-bq-data.sh --bq-project <PROJECT> --bq-dataset <DATASET> --workbench-project <PROJECT>"
USAGE="$USAGE --cdr-version=YYYYMMDD"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    --workbench-project) WORKBENCH_PROJECT=$2; shift 2;;
    --workbench-dataset) WORKBENCH_DATASET=$2; shift 2;;
    --cdr-version) CDR_VERSION=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done


if [ -z "${BQ_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BQ_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${WORKBENCH_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${WORKBENCH_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

WORKBENCH_DATASET=cdr$CDR_VERSION



# Check that bq_dataset exists and exit if not
datasets=$(bq --project=$BQ_PROJECT ls)
if [ -z "$datasets" ]
then
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi
if [[ $datasets =~ .*$BQ_DATASET.* ]]; then
  echo "$BQ_PROJECT.$BQ_DATASET exists. Good. Carrying on."
else
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi

# Check that bq_dataset exists and exit if not
datasets=$(bq --project=$BQ_PROJECT ls)
if [ -z "$datasets" ]
then
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi
re=\\b$BQ_DATASET\\b
if [[ $datasets =~ $re ]]; then
  echo "$BQ_PROJECT.$BQ_DATASET exists. Good. Carrying on."
else
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi


# Make dataset for cdr cloudsql tables
datasets=$(bq --project=$WORKBENCH_PROJECT ls)
re=\\b$WORKBENCH_DATASET\\b
if [[ $datasets =~ $re ]]; then
  echo "$WORKBENCH_DATASET exists"
else
  echo "Creating $WORKBENCH_DATASET"
  bq --project=$WORKBENCH_PROJECT mk $WORKBENCH_DATASET
fi

# Create bq tables we have json schema for
schema_path=generate-cdr/bq-schemas
create_tables=(achilles_analysis achilles_results achilles_results_concept achilles_results_dist concept concept_relationship criteria criteria_attribute domain_info survey_module domain vocabulary concept_ancestor concept_synonym)

for t in "${create_tables[@]}"
do
    bq --project=$WORKBENCH_PROJECT rm -f $WORKBENCH_DATASET.$t
    bq --quiet --project=$WORKBENCH_PROJECT mk --schema=$schema_path/$t.json $WORKBENCH_DATASET.$t
done

# Load tables from csvs we have. This is not cdr data but meta data needed for workbench app
load_tables=(domain_info survey_module achilles_analysis criteria criteria_attribute)
csv_path=generate-cdr/csv
for t in "${load_tables[@]}"
do
    bq --project=$WORKBENCH_PROJECT load --quote='"' --source_format=CSV --skip_leading_rows=1 --max_bad_records=10 $WORKBENCH_DATASET.$t $csv_path/$t.csv
done

# Populate some tables from cdr data

##########
# domain #
##########
echo "Inserting domain"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$WORKBENCH_PROJECT.$WORKBENCH_DATASET.domain\`
 (domain_id, domain_name, domain_concept_id)
SELECT domain_id, domain_name, domain_concept_id
FROM \`$BQ_PROJECT.$BQ_DATASET.domain\` d"

##############
# vocabulary #
##############
echo "Inserting vocabulary"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$WORKBENCH_PROJECT.$WORKBENCH_DATASET.vocabulary\`
 (vocabulary_id, vocabulary_name, vocabulary_reference, vocabulary_version, vocabulary_concept_id)
SELECT vocabulary_id, vocabulary_name, vocabulary_reference, vocabulary_version, vocabulary_concept_id
FROM \`$BQ_PROJECT.$BQ_DATASET.vocabulary\`"

##############
# concept_ancestor #
##############
echo "Inserting concept-ancestor"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$WORKBENCH_PROJECT.$WORKBENCH_DATASET.concept_ancestor\`
 (ancestor_concept_id, descendant_concept_id, min_levels_of_separation, max_levels_of_separation)
SELECT ancestor_concept_id, descendant_concept_id, min_levels_of_separation, max_levels_of_separation
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`"


####################
# achilles queries #
####################
# Run achilles count queries to fill achilles_results
if ./generate-cdr/run-achilles-queries.sh --bq-project $BQ_PROJECT --bq-dataset $BQ_DATASET --workbench-project $WORKBENCH_PROJECT --workbench-dataset $WORKBENCH_DATASET
then
    echo "Achilles queries ran"
else
    echo "FAILED To run achilles queries for CDR $CDR_VERSION"
    exit 1
fi

####################
# measurement queries #
####################
# Run measurement achilles count queries to fill achilles_results
if ./generate-cdr/run-measurement-queries.sh --bq-project $BQ_PROJECT --bq-dataset $BQ_DATASET --workbench-project $WORKBENCH_PROJECT --workbench-dataset $WORKBENCH_DATASET
then
    echo "Measurement achilles queries ran"
else
    echo "FAILED To run measurement achilles queries for CDR $CDR_VERSION"
    exit 1
fi

###########################
# concept with count cols #
###########################
# We can't just copy concept because the schema has a couple extra columns
# and dates need to be formatted for mysql
# Insert the base data into it formatting dates.
echo "Inserting concept table data ... "
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$WORKBENCH_PROJECT.$WORKBENCH_DATASET.concept\`
(concept_id, concept_name, domain_id, vocabulary_id, concept_class_id, standard_concept,
concept_code, count_value, prevalence, source_count_value)
SELECT c.concept_id, c.concept_name, c.domain_id, c.vocabulary_id, c.concept_class_id, c.standard_concept, c.concept_code,
0 as count_value , 0.0 as prevalence, 0 as source_count_value
from \`$BQ_PROJECT.$BQ_DATASET.concept\` c"

# Update counts and prevalence in concept
q="select count_value from \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` a where a.analysis_id = 1"
person_count=$(bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql "$q" |  tr -dc '0-9')


bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"Update \`$WORKBENCH_PROJECT.$WORKBENCH_DATASET.concept\` c
set c.source_count_value = r.source_count_value,c.count_value=r.count_value
from  (select cast(r.stratum_1 as int64) as concept_id , sum(r.count_value) as count_value , sum(r.source_count_value) as source_count_value
from \`$WORKBENCH_PROJECT.$WORKBENCH_DATASET.achilles_results\` r
where r.analysis_id in (3000,2,4,5) and CAST(r.stratum_1 as int64) > "0" group by r.stratum_1) as r
where r.concept_id = c.concept_id"


#Concept prevalence (based on count value and not on source count value)
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"Update  \`$WORKBENCH_PROJECT.$WORKBENCH_DATASET.concept\`
set prevalence =
case when count_value > 0 then round(count_value/$person_count, 2)
     when source_count_value > 0 then round(source_count_value/$person_count, 2)
     else 0.00 end
where count_value > 0 or source_count_value > 0"

##########################################
# domain info updates                    #
##########################################

# Set all_concept_count and standard_concept_count on domain_info
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.domain_info\` d
set d.all_concept_count = c.all_concept_count, d.standard_concept_count = c.standard_concept_count from
(select c.domain_id as domain_id, COUNT(DISTINCT c.concept_id) as all_concept_count,
SUM(CASE WHEN c.standard_concept IN ('S', 'C') THEN 1 ELSE 0 END) as standard_concept_count from
\`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.concept\` c
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.domain_info\` d2
on d2.domain_id = c.domain_id
and (c.count_value > 0 or c.source_count_value > 0)
group by c.domain_id) c
where d.domain_id = c.domain_id
"

# Set participant counts for each domain
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.domain_info\` d
set d.participant_count = r.count_value from
\`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\' r
where r.analysis_id = 3000 and r.stratum_1 = CAST(d.concept_id AS STRING)
and r.stratum_3 = d.domain_id
and r.stratum_2 is null
"

##########################################
# survey count updates                   #
##########################################

# Set the survey participant count on the concept
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.concept\` c1
set c1.count_value=count_val from
(select count(distinct ob.person_id) as count_val,cr.concept_id_2 as survey_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.observation\` ob
join \`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` cr
on ob.observation_source_concept_id=cr.concept_id_1 join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.survey_module\` sm
on cr.concept_id_2=sm.concept_id
group by cr.concept_id_2)
where c1.concept_id=survey_concept_id"

# Set the participant count on the survey_module row
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.survey_module\` sm
set sm.participant_count=c.count_value from
`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.concept\` c
where c.concept_id=sm.concept_id"

# Set the question count on the survey_module row
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.survey_module\` sm
set sm.question_count=num_questions from
(select count(distinct cr.concept_id_1) num_questions, cr.concept_id_2 as survey_concept_id from
`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` cr
  join `${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.survey_module\` sm2
    on cr.concept_id_2 = sm2.concept_id
where cr.relationship_id = 'Has Module'
  group by survey_concept_id)
where sm.concept_id = survey_concept_id
"

#############################################
# concept question participant count update #
#############################################
#Set the question participant counts
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.concept\` c1
set c1.count_value=count_val from
(select count(distinct ob.person_id) as count_val,cr.concept_id_2 as survey_concept_id,cr.concept_id_1 as question_id
from \`${BQ_PROJECT}.${BQ_DATASET}.observation\` ob join \`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` cr
on ob.observation_source_concept_id=cr.concept_id_1 join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.survey_module\` sm
on cr.concept_id_2 = sm.concept_id
where cr.relationship_id = 'Has Module'
group by survey_concept_id,cr.concept_id_1)
where c1.concept_id=question_id
"

########################
# concept_relationship #
########################
echo "Inserting concept_relationship"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$WORKBENCH_PROJECT.$WORKBENCH_DATASET.concept_relationship\`
 (concept_id_1, concept_id_2, relationship_id)
SELECT c.concept_id_1, c.concept_id_2, c.relationship_id
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` c"

########################
# concept_synonym #
########################
echo "Inserting concept_synonym"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$WORKBENCH_PROJECT.$WORKBENCH_DATASET.concept_synonym\`
 (id, concept_id, concept_synonym_name)
SELECT 0, c.concept_id, c.concept_synonym_name
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` c"







