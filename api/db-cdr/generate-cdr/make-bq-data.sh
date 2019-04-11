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
datasets=$(bq --project=$OUTPUT_PROJECT ls)
re=\\b$OUTPUT_DATASET\\b
if [[ $datasets =~ $re ]]; then
  echo "$OUTPUT_DATASET exists"
else
  echo "Creating $OUTPUT_DATASET"
  bq --project=$OUTPUT_PROJECT mk $OUTPUT_DATASET
fi

#Check if tables to be copied over exists in bq project dataset
# TODO:Remove criteria
tables=$(bq --project=$BQ_PROJECT --dataset=$BQ_DATASET ls)
cri_table_check=\\bcriteria\\b
cri_attr_table_check=\\bcriteria_attribute\\b
cri_rel_table_check=\\bcriteria_relationship\\b
cri_anc_table_check=\\bcriteria_ancestor\\b
cb_cri_table_check=\\bcb_criteria\\b
cb_cri_attr_table_check=\\bcb_criteria_attribute\\b
cb_cri_rel_table_check=\\bcb_criteria_relationship\\b
cb_cri_anc_table_check=\\bcb_criteria_ancestor\\b

# Create bq tables we have json schema for
# TODO:Remove criteria
schema_path=generate-cdr/bq-schemas
create_tables=(achilles_results achilles_results_concept achilles_results_dist concept concept_relationship criteria cb_criteria criteria_attribute cb_criteria_attribute criteria_relationship cb_criteria_relationship criteria_ancestor cb_criteria_ancestor domain_info survey_module domain vocabulary concept_synonym domain_vocabulary_info)

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
# TODO:Remove criteria
############
# criteria #
############
if [[ $tables =~ $cri_table_check ]]; then
    echo "Inserting criteria"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\`
     (id, parent_id, type, subtype, code, name, is_group, is_selectable, est_count, domain_id, concept_id, has_attribute, path)
    SELECT id, parent_id, type, subtype, code, name, is_group, is_selectable, est_count, domain_id, concept_id, has_attribute, path
    FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`"

    echo "Updating SNOMED PCS criteria"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` ct
    set ct.synonyms = crit.synonyms
    from (
    select c.id,
    case when c.name is null and c.code is null
    then string_agg(replace(cs.concept_synonym_name,'|','||'),'|')
    when c.name is null and c.code is not null
    then concat(c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    when c.name is not null and c.code is null
    then concat(c.name,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    else concat(c.name,'|',c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    end as synonyms
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` c
    join \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` cs
    on c.concept_id=cs.concept_id
    and type = 'SNOMED' and subtype = 'PCS'
    group by c.id, c.name, c.code) as crit
    where crit.id = ct.id"

    echo "Updating SNOMED CM criteria"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` ct
    set ct.synonyms = crit.synonyms
    from (
    select c.id,
    case when c.name is null and c.code is null
    then string_agg(replace(cs.concept_synonym_name,'|','||'),'|')
    when c.name is null and c.code is not null
    then concat(c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    when c.name is not null and c.code is null
    then concat(c.name,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    else concat(c.name,'|',c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    end as synonyms
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` c
    join \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` cs
    on c.concept_id=cs.concept_id
    and type = 'SNOMED' and subtype = 'CM'
    group by c.id, c.name, c.code) as crit
    where crit.id = ct.id"

    echo "Updating ICD9 CM criteria"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` ct
    set ct.synonyms = crit.synonyms
    from (
    select c.id,
    case when c.name is null and c.code is null
    then string_agg(replace(cs.concept_synonym_name,'|','||'),'|')
    when c.name is null and c.code is not null
    then concat(c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    when c.name is not null and c.code is null
    then concat(c.name,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    else concat(c.name,'|',c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    end as synonyms
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` c
    join \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` cs
    on c.concept_id=cs.concept_id
    and type = 'ICD9' and subtype = 'CM'
    group by c.id, c.name, c.code) as crit
    where crit.id = ct.id"

    echo "Updating ICD9 PROC criteria"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` ct
    set ct.synonyms = crit.synonyms
    from (
    select c.id,
    case when c.name is null and c.code is null
    then string_agg(replace(cs.concept_synonym_name,'|','||'),'|')
    when c.name is null and c.code is not null
    then concat(c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    when c.name is not null and c.code is null
    then concat(c.name,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    else concat(c.name,'|',c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    end as synonyms
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` c
    join \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` cs
    on c.concept_id=cs.concept_id
    and type = 'ICD9' and subtype = 'PROC'
    group by c.id, c.name, c.code) as crit
    where crit.id = ct.id"

    echo "Updating ICD10 CM criteria"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` ct
    set ct.synonyms = crit.synonyms
    from (
    select c.id,
    case when c.name is null and c.code is null
    then string_agg(replace(cs.concept_synonym_name,'|','||'),'|')
    when c.name is null and c.code is not null
    then concat(c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    when c.name is not null and c.code is null
    then concat(c.name,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    else concat(c.name,'|',c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    end as synonyms
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` c
    join \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` cs
    on c.concept_id=cs.concept_id
    and type = 'ICD10' and subtype = 'CM'
    group by c.id, c.name, c.code) as crit
    where crit.id = ct.id"

    echo "Updating ICD10 PCS criteria"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` ct
    set ct.synonyms = crit.synonyms
    from (
    select c.id,
    case when c.name is null and c.code is null
    then string_agg(replace(cs.concept_synonym_name,'|','||'),'|')
    when c.name is null and c.code is not null
    then concat(c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    when c.name is not null and c.code is null
    then concat(c.name,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    else concat(c.name,'|',c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    end as synonyms
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` c
    join \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` cs
    on c.concept_id=cs.concept_id
    and type = 'ICD10' and subtype = 'PCS'
    group by c.id, c.name, c.code) as crit
    where crit.id = ct.id"

    echo "Updating CPT criteria"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` ct
    set ct.synonyms = crit.synonyms
    from (
    select c.id,
    case when c.name is null and c.code is null
    then string_agg(replace(cs.concept_synonym_name,'|','||'),'|')
    when c.name is null and c.code is not null
    then concat(c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    when c.name is not null and c.code is null
    then concat(c.name,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    else concat(c.name,'|',c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    end as synonyms
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` c
    join \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` cs
    on c.concept_id=cs.concept_id
    and type = 'CPT'
    group by c.id, c.name, c.code) as crit
    where crit.id = ct.id"

    echo "Updating MEAS CLIN criteria"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` ct
    set ct.synonyms = crit.synonyms
    from (
    select c.id,
    case when c.name is null and c.code is null
    then string_agg(replace(cs.concept_synonym_name,'|','||'),'|')
    when c.name is null and c.code is not null
    then concat(c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    when c.name is not null and c.code is null
    then concat(c.name,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    else concat(c.name,'|',c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    end as synonyms
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` c
    join \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` cs
    on c.concept_id=cs.concept_id
    and type = 'MEAS' and subtype = 'CLIN'
    group by c.id, c.name, c.code) as crit
    where crit.id = ct.id"

    echo "Updating MEAS LAB criteria"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` ct
    set ct.synonyms = crit.synonyms
    from (
    select c.id,
    case when c.name is null and c.code is null
    then string_agg(replace(cs.concept_synonym_name,'|','||'),'|')
    when c.name is null and c.code is not null
    then concat(c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    when c.name is not null and c.code is null
    then concat(c.name,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    else concat(c.name,'|',c.code,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    end as synonyms
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` c
    join \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` cs
    on c.concept_id=cs.concept_id
    and type = 'MEAS' and subtype = 'LAB'
    group by c.id, c.name, c.code) as crit
    where crit.id = ct.id"

    echo "Updating PPI criteria"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` ct
    set ct.synonyms = crit.synonyms
    from (
    select c.id,
    case when c.name is null
    then string_agg(replace(cs.concept_synonym_name,'|','||'),'|')
    else concat(c.name,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
    end as synonyms
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` c
    join \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` cs
    on c.concept_id=cs.concept_id
    and type = 'PPI'
    group by c.id, c.name) as crit
    where crit.id = ct.id"

    echo "Updating criteria"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` ct
    set ct.synonyms = crit.synonyms
    from (
    select c.id,
    case when c.name is null and c.code is null
    then ''
    when c.name is null and c.code is not null
    then c.code
    when c.name is not null and c.code is null
    then c.name
    else concat(c.name,'|',c.code)
    end as synonyms
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` c
    join \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` cs on c.id = cs.id
    where c.type in ('MEAS','CPT','ICD10','ICD9','SNOMED')
    and cs.synonyms is null) as crit
    where crit.id = ct.id"

    echo "Updating criteria"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` ct
    set ct.synonyms = crit.synonyms
    from (
    select c.id,
    case when c.name is null
    then ''
    else c.name
    end as synonyms
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` c
    join \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` cs on c.id = cs.id
    where c.type = 'PPI'
    and cs.synonyms is null) as crit
    where crit.id = ct.id"

    echo "Updating criteria"
        bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
        "update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria\` ct
        set ct.synonyms = concat(ct.synonyms, '|', crit.synonyms)
        from (
        select min(id) as id, '[rank1]' as synonyms
        from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
        where est_count != -1
        group by name, type, subtype) as crit
        where crit.id = ct.id"
fi
# TODO:Remove criteria
######################
# criteria_attribute #
######################
if [[ $tables =~ $cri_attr_table_check ]]; then
    echo "Inserting criteria_attribute"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria_attribute\`
     (id, concept_id, value_as_concept_id, concept_name, type, est_count)
    SELECT id, concept_id, value_as_concept_id, concept_name, type, est_count
    FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_attribute\`"
fi
# TODO:Remove criteria
#########################
# criteria_relationship #
#########################
if [[ $tables =~ $cri_rel_table_check ]]; then
    echo "Inserting criteria_relationship"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria_relationship\`
     (concept_id_1, concept_id_2)
    SELECT concept_id_1, concept_id_2
    FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_relationship\`"
fi
# TODO:Remove criteria
#########################
#   criteria_ancestor   #
#########################
if [[ $tables =~ $cri_anc_table_check ]]; then
    echo "Inserting criteria_ancestor"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.criteria_ancestor\`
     (ancestor_id, descendant_id)
    SELECT ancestor_id, descendant_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor\`"
fi

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

####################
# achilles queries #
####################
# Run achilles count queries to fill achilles_results
if ./generate-cdr/run-achilles-queries.sh --bq-project $BQ_PROJECT --bq-dataset $BQ_DATASET --workbench-project $OUTPUT_PROJECT --workbench-dataset $OUTPUT_DATASET
then
    echo "Achilles queries ran"
else
    echo "FAILED To run achilles queries for CDR $CDR_VERSION"
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
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept\`
(concept_id, concept_name, domain_id, vocabulary_id, concept_class_id, standard_concept,
concept_code, count_value, prevalence, source_count_value, synonyms)
select c.concept_id, c.concept_name, c.domain_id, c.vocabulary_id, c.concept_class_id, c.standard_concept, c.concept_code,
0 as count_value , 0.0 as prevalence, 0 as source_count_value,concat(cast(c.concept_id as string),'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|')) as synonyms
from \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c join \`${BQ_PROJECT}.${BQ_DATASET}.concept_synonym\` cs
on c.concept_id=cs.concept_id group by c.concept_id,c.concept_name,c.domain_id,c.vocabulary_id,c.concept_class_id, c.standard_concept, c.concept_code"

# Update counts and prevalence in concept
q="select count_value from \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.achilles_results\` a where a.analysis_id = 1"
person_count=$(bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql "$q" |  tr -dc '0-9')

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"Update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.concept\` c
set c.source_count_value = r.source_count_value,c.count_value=r.count_value
from  (select cast(r.stratum_1 as int64) as concept_id , sum(r.count_value) as count_value , sum(r.source_count_value) as source_count_value
from \`$OUTPUT_PROJECT.$OUTPUT_DATASET.achilles_results\` r
where r.analysis_id in (3000,2,4,5) and CAST(r.stratum_1 as int64) > "0" group by r.stratum_1) as r
where r.concept_id = c.concept_id"

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
where d.domain_id = c.domain_id
"

# Set participant counts for each domain
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.domain_info\` d
set d.participant_count = r.count_value from
\`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.achilles_results\` r
where r.analysis_id = 3000 and r.stratum_1 = CAST(d.concept_id AS STRING)
and r.stratum_3 = d.domain_id
and r.stratum_2 is null"

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
where c1.concept_id=question_id
"

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
