#!/bin/bash

# This generates big query count databases cdr that get put in cloudsql for workbench

set -xeuo pipefail
IFS=$'\n\t'

# get options
# --project=all-of-us-workbench-test *required

# --cdr=cdr_version ... *optional
USAGE="./generate-clousql-cdr/make-bq-data.sh --bq-project <PROJECT> --bq-dataset <DATASET> --output-project <PROJECT> --output-dataset <DATASET>"
USAGE="$USAGE --cdr-version=YYYYMMDD"

while [ $# -gt 0 ]; do
  echo "1 is $1"

  case "$1" in
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    --output-project) OUTPUT_PROJECT=$2; shift 2;;
    --output-dataset) OUTPUT_DATASET=$2; shift 2;;
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

if [ -z "${OUTPUT_PROJECT}" ] || [ -z "${OUTPUT_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

# Check that bq_dataset exists and exit if not
datasets=$(bq --project=$BQ_PROJECT ls --max_results=150)
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
datasets=$(bq --project=$OUTPUT_PROJECT ls --max_results=150)
re=\\b$OUTPUT_DATASET\\b
if [[ $datasets =~ $re ]]; then
  echo "$OUTPUT_DATASET exists"
else
  echo "Creating $OUTPUT_DATASET"
  bq --project=$OUTPUT_PROJECT mk $OUTPUT_DATASET
fi

#Check if tables to be copied over exists in bq project dataset
tables=$(bq --project=$BQ_PROJECT --dataset=$BQ_DATASET ls --max_results=150)
cb_cri_table_check=\\bcb_criteria\\b
cb_cri_attr_table_check=\\bcb_criteria_attribute\\b
cb_cri_rel_table_check=\\bcb_criteria_relationship\\b
cb_cri_anc_table_check=\\bcb_criteria_ancestor\\b

# Create bq tables we have json schema for
schema_path=generate-cdr/bq-schemas
create_tables=(concept concept_relationship cb_criteria cb_criteria_attribute cb_criteria_relationship cb_criteria_ancestor domain_info survey_module domain vocabulary concept_synonym domain_vocabulary_info)

for t in "${create_tables[@]}"
do
    bq --project=$OUTPUT_PROJECT rm -f $OUTPUT_DATASET.$t
    bq --quiet --project=$OUTPUT_PROJECT mk --schema=$schema_path/$t.json $OUTPUT_DATASET.$t
done

# Load tables from csvs we have. This is not cdr data but meta data needed for workbench app
load_tables=(domain_info survey_module)
csv_path=generate-cdr/csv
for t in "${load_tables[@]}"
do
    bq --project=$OUTPUT_PROJECT load --quote='"' --source_format=CSV --skip_leading_rows=1 --max_bad_records=10 $OUTPUT_DATASET.$t $csv_path/$t.csv
done

# Populate some tables from cdr data
###############
# cb_criteria #
###############
if [[ $tables =~ $cb_cri_table_check ]]; then
    echo "Inserting cb_criteria"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\`
     (id, parent_id, domain_id, type, subtype, is_standard, code, name, value, is_group, is_selectable, est_count, concept_id, has_attribute, has_hierarchy, has_ancestor_data, path, synonyms)
    SELECT id, parent_id, domain_id, type, subtype, is_standard, code, name, value, is_group, is_selectable, est_count, concept_id, has_attribute, has_hierarchy, has_ancestor_data, path, synonyms
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`"
fi

#########################
# cb_criteria_attribute #
#########################
if [[ $tables =~ $cb_cri_attr_table_check ]]; then
    echo "Inserting cb_criteria_attribute"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria_attribute\`
     (id, concept_id, value_as_concept_id, concept_name, type, est_count)
    SELECT id, concept_id, value_as_concept_id, concept_name, type, est_count
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute\`"
fi

############################
# cb_criteria_relationship #
############################
if [[ $tables =~ $cb_cri_rel_table_check ]]; then
    echo "Inserting cb_criteria_relationship"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria_relationship\`
     (concept_id_1, concept_id_2)
    SELECT concept_id_1, concept_id_2
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_relationship\`"
fi

############################
#   cb_criteria_ancestor   #
############################
if [[ $tables =~ $cb_cri_anc_table_check ]]; then
    echo "Inserting cb_criteria_ancestor"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria_ancestor\`
     (ancestor_id, descendant_id)
    SELECT ancestor_id, descendant_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_ancestor\`"
fi

##########
# domain #
##########
echo "Inserting domain"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain\`
 (domain_id, domain_name, domain_concept_id)
SELECT domain_id, domain_name, domain_concept_id
FROM \`$BQ_PROJECT.$BQ_DATASET.domain\` d"

##############
# vocabulary #
##############
echo "Inserting vocabulary"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.vocabulary\`
 (vocabulary_id, vocabulary_name, vocabulary_reference, vocabulary_version, vocabulary_concept_id)
SELECT vocabulary_id, vocabulary_name, vocabulary_reference, vocabulary_version, vocabulary_concept_id
FROM \`$BQ_PROJECT.$BQ_DATASET.vocabulary\`"

###########################
# concept with count cols #
###########################
# We can't just copy concept because the schema has a couple extra columns
# and dates need to be formatted for mysql
# Insert the base data into it formatting dates.
echo "Inserting concept table data ... "
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept\`
(concept_id, concept_name, domain_id, vocabulary_id, concept_class_id, standard_concept,
concept_code, count_value, prevalence, source_count_value, synonyms)
select c.concept_id, c.concept_name, c.domain_id, c.vocabulary_id, c.concept_class_id, c.standard_concept, c.concept_code,
0 as count_value , 0.0 as prevalence, 0 as source_count_value,concat(cast(c.concept_id as string),'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|')) as synonyms
from \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c
left join \`${BQ_PROJECT}.${BQ_DATASET}.concept_synonym\` cs
on c.concept_id=cs.concept_id
group by c.concept_id,c.concept_name,c.domain_id,c.vocabulary_id,c.concept_class_id, c.standard_concept, c.concept_code"

# Update counts and prevalence in concept
q="select count(*) from \`$BQ_PROJECT.$BQ_DATASET.person\`"
person_count=$(bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql "$q" |  tr -dc '0-9')

# Update counts in concept for gender
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"Update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept\` c
set c.source_count_value = r.source_count_value,
c.count_value = r.count_value
from  (select gender_concept_id as concept_id,
COUNT(distinct person_id) as count_value,
0 as source_count_value
from \`$BQ_PROJECT.$BQ_DATASET.person\`
group by GENDER_CONCEPT_ID) as r
where r.concept_id = c.concept_id"

# Update counts in concept for race
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"Update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept\` c
set c.source_count_value = r.source_count_value,
c.count_value = r.count_value
from  (select RACE_CONCEPT_ID AS concept_id,
COUNT(distinct person_id) as count_value,
0 as source_count_value
from \`$BQ_PROJECT.$BQ_DATASET.person\`
group by RACE_CONCEPT_ID) as r
where r.concept_id = c.concept_id"

# Update counts in concept for ethnicity
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"Update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept\` c
set c.source_count_value = r.source_count_value,
c.count_value = r.count_value
from  (select ETHNICITY_CONCEPT_ID AS concept_id,
COUNT(distinct person_id) as count_value,
0 as source_count_value
from \`$BQ_PROJECT.$BQ_DATASET.person\`
group by ETHNICITY_CONCEPT_ID) as r
where r.concept_id = c.concept_id"

# Update counts in concept for visit domain
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"Update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept\` c
set c.source_count_value = (c.source_count_value + r.source_count_value),
c.count_value = (c.count_value + r.count_value)
from  (select vo1.visit_concept_id as concept_id,
COUNT(distinct vo1.PERSON_ID) as count_value,
(select COUNT(distinct vo2.person_id) from \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` vo2 where vo2.visit_source_concept_id=vo1.visit_concept_id) as source_count_value
from \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` vo1
where vo1.visit_concept_id > 0
group by vo1.visit_concept_id
union all
select vo1.visit_source_concept_id as concept_id,
COUNT(distinct vo1.PERSON_ID) as count_value,
COUNT(distinct vo1.PERSON_ID) as source_count_value
from \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` vo1
where vo1.visit_source_concept_id not in (select distinct visit_concept_id from \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\`)
group by vo1.visit_source_concept_id) as r
where r.concept_id = c.concept_id"

# Update counts in concept for condition domain
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"Update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept\` c
set c.source_count_value = (c.source_count_value + r.source_count_value),
c.count_value = (c.count_value + r.count_value)
from  (select co1.condition_CONCEPT_ID as concept_id,
COUNT(distinct co1.PERSON_ID) as count_value,
(select COUNT(distinct co2.person_id) from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` co2 where co2.condition_source_concept_id=co1.condition_concept_id) as source_count_value
from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` co1
where co1.condition_concept_id > 0
and co1.condition_concept_id != 19
group by co1.condition_CONCEPT_ID
union all
select co1.condition_source_concept_id AS concept_id,
COUNT(distinct co1.PERSON_ID) as count_value,
COUNT(distinct co1.PERSON_ID) as source_count_value
from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` co1
where co1.condition_source_concept_id not in (select distinct condition_concept_id from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\`)
and co1.condition_source_concept_id != 19
group by co1.condition_source_concept_id) as r
where r.concept_id = c.concept_id"

# Update counts in concept for procedure domain
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"Update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept\` c
set c.source_count_value = (c.source_count_value + r.source_count_value),
c.count_value = (c.count_value + r.count_value)
from  (select po1.procedure_CONCEPT_ID AS concept_id,
COUNT(distinct po1.PERSON_ID) as count_value,
(select COUNT(distinct po2.PERSON_ID) from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` po2 where po2.procedure_source_CONCEPT_ID=po1.procedure_concept_id) as source_count_value
from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` po1
where po1.procedure_concept_id > 0 and po1.procedure_concept_id != 10
group by po1.procedure_CONCEPT_ID
union all
select po1.procedure_source_CONCEPT_ID AS concept,
COUNT(distinct po1.PERSON_ID) as count_value,
COUNT(distinct po1.PERSON_ID) as source_count_value from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` po1 where
po1.procedure_source_concept_id not in (select distinct procedure_concept_id from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\`)
and po1.procedure_source_concept_id != 10
group by po1.procedure_source_CONCEPT_ID) as r
where r.concept_id = c.concept_id"

# Update counts in concept for drug domain
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"Update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept\` c
set c.source_count_value = (c.source_count_value + r.source_count_value),
c.count_value = (c.count_value + r.count_value)
from  (select de1.drug_CONCEPT_ID AS concept_id,
COUNT(distinct de1.PERSON_ID) as count_value,
(select COUNT(distinct de2.PERSON_ID) from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` de2 where de2.drug_source_concept_id=de1.drug_concept_id) as source_count_value
from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` de1
where de1.drug_concept_id > 0
group by de1.drug_CONCEPT_ID
union all
select de1.drug_source_CONCEPT_ID AS concept_id,
COUNT(distinct de1.PERSON_ID) as count_value,
COUNT(distinct de1.PERSON_ID) as source_count_value from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` de1 where
de1.drug_source_concept_id not in (select distinct drug_concept_id from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`)
group by de1.drug_source_CONCEPT_ID) as r
where r.concept_id = c.concept_id"

# Update counts in concept for observation domain
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"Update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept\` c
set c.source_count_value = (c.source_count_value + r.source_count_value),
c.count_value = (c.count_value + r.count_value)
from  (select co1.observation_CONCEPT_ID AS concept_id,
COUNT(distinct co1.PERSON_ID) as count_value,
(select COUNT(distinct co2.PERSON_ID) from \`$BQ_PROJECT.$BQ_DATASET.observation\` co2 where co2.observation_source_concept_id=co1.observation_concept_id) as source_count_value
from \`$BQ_PROJECT.$BQ_DATASET.observation\` co1
where co1.observation_concept_id > 0
group by co1.observation_CONCEPT_ID
union all
select co1.observation_source_CONCEPT_ID AS concept_id,
COUNT(distinct co1.PERSON_ID) as count_value,
COUNT(distinct co1.PERSON_ID) as source_count_value
from \`$BQ_PROJECT.$BQ_DATASET.observation\` co1 where co1.observation_source_concept_id > 0 and
co1.observation_source_concept_id not in (select distinct observation_concept_id from \`$BQ_PROJECT.$BQ_DATASET.observation\`)
group by co1.observation_source_CONCEPT_ID) as r
where r.concept_id = c.concept_id"

# Update counts in concept for measurement domain
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"Update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept\` c
set c.source_count_value = (c.source_count_value + r.source_count_value),
c.count_value = (c.count_value + r.count_value)
from  (select co1.measurement_concept_id AS concept_id,
COUNT(distinct co1.PERSON_ID) as count_value,
(select COUNT(distinct co2.person_id) from \`$BQ_PROJECT.$BQ_DATASET.measurement\` co2 where co2.measurement_source_concept_id=co1.measurement_concept_id) as source_count_value
from \`$BQ_PROJECT.$BQ_DATASET.measurement\` co1
where co1.measurement_concept_id > 0 and co1.measurement_concept_id != 21
group by co1.measurement_concept_id
union all
select co1.measurement_source_concept_id AS concept_id,
COUNT(distinct co1.PERSON_ID) as count_value,
COUNT(distinct co1.PERSON_ID) as source_count_value
from \`$BQ_PROJECT.$BQ_DATASET.measurement\` co1
where co1.measurement_source_concept_id not in (select distinct measurement_concept_id from \`$BQ_PROJECT.$BQ_DATASET.measurement\`)
and co1.measurement_source_concept_id != 21
group by co1.measurement_source_concept_id) as r
where r.concept_id = c.concept_id"

# We're using *_ext tables to limit the count of people that have EHR specific data. This a an effort to exclude PPI related data.
# PPI specific data is counted in the survey_module table. The only time the *_ext tables should be absent is in the synthetic datasets.
if [[ "$tables" == *"_ext"* ]]; then
    # Set participant counts for Condition concept
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.concept\` c
    set c.count_value = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` co
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on co.condition_concept_id = c.concept_id
    join \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence_ext\` mc on co.condition_occurrence_id = mc.condition_occurrence_id
    where c.vocabulary_id != 'PPI'
    and mc.src_id in (select distinct src_id from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence_ext\` where src_id like '%EHR%')) as r
    where c.concept_id = 19"

    # Set participant counts for Measurement domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.concept\` c
    set c.count_value = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on m.measurement_concept_id = c.concept_id
    join \`$BQ_PROJECT.$BQ_DATASET.measurement_ext\` mm on m.measurement_id = mm.measurement_id
    where c.vocabulary_id != 'PPI'
    and mm.src_id in (select distinct src_id from \`$BQ_PROJECT.$BQ_DATASET.measurement_ext\` where src_id like '%EHR%')) as r
    where c.concept_id = 21"

    # Set participant counts for Procedure domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.concept\` c
    set c.count_value = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` po
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on po.procedure_concept_id = c.concept_id
    join \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence_ext\` pm on po.procedure_occurrence_id = pm.procedure_occurrence_id
    where c.vocabulary_id != 'PPI'
    and pm.src_id in (select distinct src_id from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence_ext\` where src_id like '%EHR%')) as r
    where c.concept_id = 10"

    # Set participant counts for Drug domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.concept\` c
    set c.count_value = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` de
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on de.drug_concept_id = c.concept_id
    join \`$BQ_PROJECT.$BQ_DATASET.drug_exposure_ext\` md on de.drug_exposure_id = md.drug_exposure_id
    where c.vocabulary_id != 'PPI'
    and md.src_id in (select distinct src_id from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure_ext\` where src_id like '%EHR%')) as r
    where c.concept_id = 13"

    # Set participant counts for Observation domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.concept\` c
    set c.count_value = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.observation\` o
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on o.observation_concept_id = c.concept_id
    join \`$BQ_PROJECT.$BQ_DATASET.observation_ext\` od on o.observation_id = od.observation_id
    where c.vocabulary_id != 'PPI'
    and od.src_id in (select distinct src_id from \`$BQ_PROJECT.$BQ_DATASET.observation_ext\` where src_id like '%EHR%')) as r
    where c.concept_id = 27"
else
    # Set participant counts for Condition domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.concept\` c
    set c.count_value = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` co
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on co.condition_concept_id = c.concept_id
    where c.vocabulary_id != 'PPI') as r
    where c.concept_id = 19"

    # Set participant counts for Measurement domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.concept\` c
    set c.count_value = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on m.measurement_concept_id = c.concept_id
    where c.vocabulary_id != 'PPI'
    and m.measurement_concept_id not in (3036277,903118,903115,3025315,903135,903136,903126,903111,42528957)) as r
    where c.concept_id = 21"

    # Set participant counts for Procedure domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.concept\` c
    set c.count_value = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` po
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on po.procedure_concept_id = c.concept_id
    where c.vocabulary_id != 'PPI'
    and c.concept_id not in (3036277,903133,903118,903115,3025315,903121,903135,903136,903126,903111,42528957,903120)) as r
    where c.concept_id = 10"

    # Set participant counts for Drug domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.concept\` c
    set c.count_value = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` de
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on de.drug_concept_id = c.concept_id
    where c.vocabulary_id != 'PPI') as r
    where c.concept_id = 13"

    # Set participant counts for Observation domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.concept\` c
    set c.count_value = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.observation\` o
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on o.observation_concept_id = c.concept_id
    where c.vocabulary_id != 'PPI') as r
    where c.concept_id = 27"
fi

#Concept prevalence (based on count value and not on source count value)
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"Update  \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept\`
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
"update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.domain_info\` d
set d.all_concept_count = c.all_concept_count, d.standard_concept_count = c.standard_concept_count from
(select c.domain_id as domain_id, COUNT(DISTINCT c.concept_id) as all_concept_count,
SUM(CASE WHEN c.standard_concept IN ('S', 'C') THEN 1 ELSE 0 END) as standard_concept_count from
\`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.concept\` c
join \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.domain_info\` d2
on d2.domain_id = c.domain_id
and (c.count_value > 0 or c.source_count_value > 0)
group by c.domain_id) c
where d.domain_id = c.domain_id"

# domain_info contains counts for all EHR specific domains (Condition, Measurement, Procedure and Drug Exposure
# We're using *_ext tables to limit the count of people that have EHR specific data. This a an effort to exclude PPI related data.
# PPI specific data is counted in the survey_module table. The only time the *_ext tables should be absent is in the synthetic datasets.
if [[ "$tables" == *"_ext"* ]]; then
    # Set participant counts for Condition domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.domain_info\` d
    set d.participant_count = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` co
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on co.condition_concept_id = c.concept_id
    join \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence_ext\` mc on co.condition_occurrence_id = mc.condition_occurrence_id
    where c.vocabulary_id != 'PPI'
    and mc.src_id in (select distinct src_id from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence_ext\` where src_id like '%EHR%')) as r
    where d.concept_id = 19"

    # Set participant counts for Measurement domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.domain_info\` d
    set d.participant_count = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on m.measurement_concept_id = c.concept_id
    join \`$BQ_PROJECT.$BQ_DATASET.measurement_ext\` mm on m.measurement_id = mm.measurement_id
    where c.vocabulary_id != 'PPI'
    and mm.src_id in (select distinct src_id from \`$BQ_PROJECT.$BQ_DATASET.measurement_ext\` where src_id like '%EHR%')) as r
    where d.concept_id = 21"

    # Set participant counts for Procedure domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.domain_info\` d
    set d.participant_count = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` po
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on po.procedure_concept_id = c.concept_id
    join \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence_ext\` pm on po.procedure_occurrence_id = pm.procedure_occurrence_id
    where c.vocabulary_id != 'PPI'
    and pm.src_id in (select distinct src_id from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence_ext\` where src_id like '%EHR%')) as r
    where d.concept_id = 10"

    # Set participant counts for Drug domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.domain_info\` d
    set d.participant_count = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` de
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on de.drug_concept_id = c.concept_id
    join \`$BQ_PROJECT.$BQ_DATASET.drug_exposure_ext\` md on de.drug_exposure_id = md.drug_exposure_id
    where c.vocabulary_id != 'PPI'
    and md.src_id in (select distinct src_id from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure_ext\` where src_id like '%EHR%')) as r
    where d.concept_id = 13"

    # Set participant counts for Observation domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.domain_info\` d
    set d.participant_count = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.observation\` o
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on o.observation_concept_id = c.concept_id
    join \`$BQ_PROJECT.$BQ_DATASET.observation_ext\` od on o.observation_id = od.observation_id
    where c.vocabulary_id != 'PPI'
    and od.src_id in (select distinct src_id from \`$BQ_PROJECT.$BQ_DATASET.observation_ext\` where src_id like '%EHR%')) as r
    where d.concept_id = 27"
else
    # Set participant counts for Condition domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.domain_info\` d
    set d.participant_count = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` co
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on co.condition_concept_id = c.concept_id
    where c.vocabulary_id != 'PPI') as r
    where d.concept_id = 19"

    # Set participant counts for Measurement domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.domain_info\` d
    set d.participant_count = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on m.measurement_concept_id = c.concept_id
    where c.vocabulary_id != 'PPI'
    and m.measurement_concept_id not in (3036277,903118,903115,3025315,903135,903136,903126,903111,42528957)) as r
    where d.concept_id = 21"

    # Set participant counts for Procedure domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.domain_info\` d
    set d.participant_count = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` po
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on po.procedure_concept_id = c.concept_id
    where c.vocabulary_id != 'PPI'
    and c.concept_id not in (3036277,903133,903118,903115,3025315,903121,903135,903136,903126,903111,42528957,903120)) as r
    where d.concept_id = 10"

    # Set participant counts for Drug domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.domain_info\` d
    set d.participant_count = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` de
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on de.drug_concept_id = c.concept_id
    where c.vocabulary_id != 'PPI') as r
    where d.concept_id = 13"

    # Set participant counts for Observation domain
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.domain_info\` d
    set d.participant_count = r.count from
    (select count(distinct person_id) as count
    from \`$BQ_PROJECT.$BQ_DATASET.observation\` o
    join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on o.observation_concept_id = c.concept_id
    where c.vocabulary_id != 'PPI') as r
    where d.concept_id = 27"
fi

##########################################
# survey count updates                   #
##########################################

# Set the survey participant count on the concept
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.concept\` c1
set c1.count_value=count_val from
(select count(distinct ob.person_id) as count_val,cr.concept_id_2 as survey_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.observation\` ob
join \`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` cr
on ob.observation_source_concept_id=cr.concept_id_1 join \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.survey_module\` sm
on cr.concept_id_2=sm.concept_id
group by cr.concept_id_2)
where c1.concept_id=survey_concept_id"

# Set the participant count on the survey_module row
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.survey_module\` sm
set sm.participant_count=c.count_value from
\`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.concept\` c
where c.concept_id=sm.concept_id"

# Set the question participant counts
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.concept\` c1
set c1.count_value=count_val from
(select count(distinct ob.person_id) as count_val,cr.concept_id_2 as survey_concept_id,cr.concept_id_1 as question_id
from \`${BQ_PROJECT}.${BQ_DATASET}.observation\` ob join \`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` cr
on ob.observation_source_concept_id=cr.concept_id_1 join \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.survey_module\` sm
on cr.concept_id_2 = sm.concept_id
where cr.relationship_id = 'Has Module'
group by survey_concept_id,cr.concept_id_1)
where c1.concept_id=question_id"

# Set the question count on the survey_module row
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.survey_module\` sm
set sm.question_count=num_questions from
(select cr.ancestor_concept_id,c1.concept_name,count(*) as num_questions
 from \`${BQ_PROJECT}.${BQ_DATASET}.concept_ancestor\` cr
 join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on c.concept_id=cr.descendant_concept_id
 join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c1 on c1.concept_id=cr.ancestor_concept_id
 where cr.ancestor_concept_id in (1586134, 1585855, 1585710)
 and c.concept_class_id='Question'
 group by cr.ancestor_concept_id,c1.concept_name)
where sm.concept_id = ancestor_concept_id"

########################
# concept_relationship #
########################
echo "Inserting concept_relationship"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept_relationship\`
 (concept_id_1, concept_id_2, relationship_id)
SELECT c.concept_id_1, c.concept_id_2, c.relationship_id
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` c"

########################
# concept_synonym #
########################
echo "Inserting concept_synonym"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept_synonym\`
 (id, concept_id, concept_synonym_name)
SELECT 0, c.concept_id, c.concept_synonym_name
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` c"

###########################
# Domain_Vocabulary_Info #
###########################
echo "Updating all concept count in domain_vocabulary_info"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_vocabulary_info\`
(domain_id,vocabulary_id,all_concept_count,standard_concept_count)
select d2.domain_id as domain_id,c.vocabulary_id as vocabulary_id, COUNT(DISTINCT c.concept_id) as all_concept_count,
COUNT(DISTINCT CASE WHEN c.standard_concept IN ('S', 'C') THEN c.concept_id ELSE NULL END) as standard_concept_count from
\`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept\` c
join \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain\` d2
on d2.domain_id = c.domain_id
and (c.count_value > 0 or c.source_count_value > 0)
group by d2.domain_id,c.vocabulary_id"
