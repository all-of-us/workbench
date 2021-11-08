#!/bin/bash
# do not output cmd-line for now
set -e
SQL_FOR='DRUG_EXPOSURE - ATC/RXNORM'
TBL_CBC='cb_criteria'
TBL_PCA='prep_concept_ancestor'
TBL_CBA='cb_criteria_ancestor'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset
ID_PREFIX=$3

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
function createTmpTable(){
  local tmpTbl="prep_temp_"$1"_"$ID_PREFIX
  res=$(bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
    "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.$tmpTbl\` AS
      SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.$1\` LIMIT 0")
  echo $res >&2
  echo "$tmpTbl"
}
function cpToMain(){
  local tbl_to=`echo "$1" | sed -e 's/prep_temp_\(.*\)_[0-9]*/\1/'`
  bq cp --append_table=true --quiet --project_id=$BQ_PROJECT \
     $BQ_DATASET.$1 $BQ_DATASET.$tbl_to
}
echo "Running in parallel and Multitable mode - " "$ID_PREFIX - $SQL_FOR"
CB_CRITERIA_START_ID=$[$ID_PREFIX*10**9] # 3  billion
CB_CRITERIA_END_ID=$[$[ID_PREFIX+1]*10**9] # 4  billion
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
TBL_PCA=$(createTmpTable $TBL_PCA)
TBL_CBA=$(createTmpTable $TBL_CBA)
####### end common block ###########
# make-cb-criteria-drug-rxnorm.sh
#4659 - #4979: make-bq-criteria-tables.sh
# ---------ORDER - 17 - DRUG_EXPOSURE - ATC/RXNORM ---------
#ORDER - 17: #4659 - #4979: - DRUG_EXPOSURE - ATC/RXNORM ---------
# 17.0: #4659:  DRUGS - add roots
  # cb_criteria: #4659: Uses : cb_criteria, concept
# 17.1: #4672: DRUGS - add root for unmapped ingredients
  #cb_criteria: Uses : cb_criteria
# 17.2: #4680:  DRUGS - level 2
  #cb_criteria: Uses : cb_criteria, prep_atc_rel_in_data
# 17.3: #4701:  DRUGS - level 3
    #cb_criteria: Uses : cb_criteria, prep_atc_rel_in_data
# 17.4: #4722:  DRUGS - level 4
    #cb_criteria: Uses : cb_criteria, prep_atc_rel_in_data
#17.5: #4743:  DRUGS - level 5 - ingredients
    #cb_criteria: Uses : cb_criteria, prep_atc_rel_in_data
# 17.6: #4764:  DRUGS - add parents for unmapped ingredients
    #cb_criteria: Uses : cb_criteria, concept, concept_ancestor, drug_exposure
# 17.7: #4800:  DRUGS - add ingredients
    #cb_criteria: Uses : cb_criteria, concept, concept_ancestor, drug_exposure
# 17.8: #4800:  DRUGS - generate child counts
    #cb_criteria:  #4843: Uses : cb_criteria, drug_exposure, concept_ancestor
# 17.9: #4867:  DRUG_EXPOSURE - ATC/RXNORM - add brand names
    #cb_criteria:  #4867: Uses : cb_criteria, concept, concept_relationship
# prep_concept_ancestor: #4917: Uses : cb_criteria
# cb_criteria parent counts: #4934: Uses : cb_criteria, prep_concept_ancestor, drug_exposure, concept_ancestor, concept
# add to make-cb-criteria-17-drug-rxnorm-other.sh
# ---------CB_CRITERIA_ANCESTOR ---------
#5954 - #5982 cb_criteria_ancestor: Uses tables: concept_ancestor, cb_criteria, drug_exposure
################################################
# DRUG_EXPOSURE - ATC/RXNORM
################################################
echo "DRUGS - add roots"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,concept_id
        ,code,name
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,has_ancestor_data
        ,path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY concept_code)
       + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
    , 0
    , 'DRUG'
    , 1
    , 'ATC'
    , concept_id
    , concept_code
    , CONCAT( UPPER(SUBSTR(concept_name, 1, 1)), LOWER(SUBSTR(concept_name, 2)) )
    , 1
    , 0
    , 0
    , 1
    , 1
    , CAST(ROW_NUMBER() OVER(order by concept_code)
         + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as STRING)
FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
WHERE VOCABULARY_ID = 'ATC'
    and CONCEPT_CLASS_ID = 'ATC 1st'
    and STANDARD_CONCEPT = 'C'"

echo "DRUGS - add root for unmapped ingredients"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,name
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,has_ancestor_data
        ,path
    )
SELECT
    (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) + 1
    , 0
    , 'DRUG'
    , 1
    , 'ATC'
    , 'Unmapped ingredients'
    , 1
    , 0
    , 0
    , 1
    , 1
    , CAST((SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) + 1 as STRING)"

echo "DRUGS - level 2"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,concept_id
        ,code
        ,name
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,has_ancestor_data
        ,path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code)
      + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
    , p.id
    , 'DRUG'
    , 1
    , 'ATC'
    , c.concept_id
    , c.concept_code
    , CONCAT( UPPER(SUBSTR(c.concept_name, 1, 1)), LOWER(SUBSTR(c.concept_name, 2)) )
    , 1
    , 1
    , 0
    , 1
    , 1
    , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code)
         + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` p
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\` c on p.code = c.p_concept_code
WHERE p.domain_id = 'DRUG'
    and p.type = 'ATC'
    and p.id not in
        (
            SELECT parent_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
            WHERE domain_id = 'DRUG'
                and type = 'ATC'
                and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
        )"

echo "DRUGS - level 3"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,concept_id
        ,code,name
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,has_ancestor_data
        ,path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code)
      + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
    , p.id
    , 'DRUG'
    , 1
    , 'ATC'
    , c.concept_id
    , c.concept_code
    , CONCAT( UPPER(SUBSTR(c.concept_name, 1, 1)), LOWER(SUBSTR(c.concept_name, 2)) )
    , 1
    , 1
    , 0
    , 1
    , 1
    , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code)
         + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` p
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\` c on p.code = c.p_concept_code
WHERE p.domain_id = 'DRUG'
    and p.type = 'ATC'
    and p.id not in
        (
            SELECT parent_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
            WHERE domain_id = 'DRUG'
                and type = 'ATC'
                and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
        )"

echo "DRUGS - level 4"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type,concept_id
        ,code
        ,name
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,has_ancestor_data
        ,path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code)
      + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
    , p.id
    , 'DRUG'
    , 1
    , 'ATC'
    , c.concept_id
    , c.concept_code
    , CONCAT( UPPER(SUBSTR(c.concept_name, 1, 1)), LOWER(SUBSTR(c.concept_name, 2)) )
    , 1
    , 1
    , 0
    , 1
    , 1
    , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code)
         + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` p
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\` c on p.code = c.p_concept_code
WHERE p.domain_id = 'DRUG'
    and p.type = 'ATC'
    and p.id not in
        (
            SELECT parent_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
            WHERE domain_id = 'DRUG'
                and type = 'ATC'
                and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
        )"

echo "DRUGS - level 5 - ingredients"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,concept_id
        ,code,name
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,has_ancestor_data
        ,path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY p.id, UPPER(c.concept_name))
      + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
    , p.id
    , 'DRUG'
    , 1
    , 'RXNORM'
    , c.concept_id
    , c.concept_code
    , CONCAT( UPPER(SUBSTR(c.concept_name, 1, 1)), LOWER(SUBSTR(c.concept_name, 2)) )
    , 0
    , 1
    , 0
    , 1
    , 1
    , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, UPPER(c.concept_name))
         + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` p
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\` c on p.code = c.p_concept_code
WHERE p.domain_id = 'DRUG'
    and p.type = 'ATC'
    and p.id not in
        (
            SELECT parent_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
            WHERE domain_id = 'DRUG'
                and type = 'ATC'
                and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
        )"

echo "DRUGS - add parents for unmapped ingredients"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,code
        ,name
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,has_ancestor_data
        ,path
    )
SELECT
    ROW_NUMBER() OVER(ORDER BY UPPER(name))
      + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
    , (SELECT id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`  WHERE name = 'Unmapped ingredients' and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
    , 'DRUG'
    , 1
    , 'ATC'
    , name as code
    , name
    , 1
    , 0
    , 0
    , 1
    , 1
    , CONCAT( (SELECT CAST(id as STRING) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
         WHERE name = 'Unmapped ingredients' and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID),'.'
         , CAST(ROW_NUMBER() OVER(ORDER BY UPPER(name))
         + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as STRING))
FROM
    (
        SELECT distinct UPPER(SUBSTR(concept_name, 1, 1)) name
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
        WHERE VOCABULARY_ID in  ('RxNorm', 'RxNorm Extension')
            and CONCEPT_CLASS_ID = 'Ingredient'
            and STANDARD_CONCEPT = 'S'
            and concept_id in
                (
                    SELECT ANCESTOR_CONCEPT_ID
                    FROM \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
                    WHERE DESCENDANT_CONCEPT_ID in
                        (
                            SELECT distinct DRUG_CONCEPT_ID
                            FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`
                        )
                )
            and concept_id not in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                    WHERE domain_id = 'DRUG'
                        and concept_id is not null
                        and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
                )
    )"

echo "DRUGS - add unmapped ingredients"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,concept_id
        ,code,name
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,has_ancestor_data
        ,path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY z.id, UPPER(x.concept_name))
      + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
    , z.id
    , 'DRUG'
    , 1
    , 'RXNORM'
    , x.concept_id
    , x.concept_code
    , CONCAT( UPPER(SUBSTR(x.concept_name, 1, 1)), LOWER(SUBSTR(x.concept_name, 2)) )
    , 0
    , 1
    , 0
    , 1
    , 1
    , CONCAT(z.path, '.', CAST(ROW_NUMBER() OVER(order by z.id, UPPER(x.concept_name))
         + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as STRING))
FROM
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
        WHERE VOCABULARY_ID in  ('RxNorm', 'RxNorm Extension')
            and CONCEPT_CLASS_ID = 'Ingredient'
            and STANDARD_CONCEPT = 'S'
            and concept_id in
                (
                    SELECT ANCESTOR_CONCEPT_ID
                    FROM \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
                    WHERE DESCENDANT_CONCEPT_ID in
                        (
                            SELECT distinct DRUG_CONCEPT_ID
                            FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`
                        )
                )
            and concept_id not in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                    WHERE domain_id = 'DRUG'
                        and concept_id is not null
                        and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
                )
    ) x
JOIN
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
        WHERE domain_id = 'DRUG'
            and type = 'ATC'
            and length(name) = 1
            and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
    ) z on UPPER(SUBSTR(x.concept_name, 1, 1)) = z.name"

echo "DRUGS - generate child counts"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
set x.rollup_count = 0
    , x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT b.ANCESTOR_CONCEPT_ID as concept_id, count(distinct a.person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a
        JOIN
            (
                SELECT *
                FROM \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\` x
                left JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` y on x.ANCESTOR_CONCEPT_ID = y.CONCEPT_ID
                WHERE VOCABULARY_ID in  ('RxNorm', 'RxNorm Extension')
                    and CONCEPT_CLASS_ID = 'Ingredient'
            ) b on a.DRUG_CONCEPT_ID = b.DESCENDANT_CONCEPT_ID
        group by 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'DRUG'
    and x.type = 'RXNORM'
    and x.id > $CB_CRITERIA_START_ID and x.id < $CB_CRITERIA_END_ID"

echo "DRUG_EXPOSURE - ATC/RXNORM - add brand names"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , concept_id
        , code
        , name
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
      ROW_NUMBER() OVER(ORDER BY UPPER(concept_name))
         + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
    , -1
    , 'DRUG'
    , 1
    , 'BRAND'
    , concept_id
    , concept_code
    , concept_name
    , 0
    , 1
    , 0
    , 0
FROM
    (
        SELECT DISTINCT b.concept_id, b.concept_name, b.concept_code
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.concept_id_1 = b.concept_id --brands
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on a.concept_id_2 = c.concept_id --ingredients
        WHERE b.vocabulary_id in ('RxNorm','RxNorm Extension')
            and b.concept_class_id = 'Brand Name'
            and b.invalid_reason is null
            and c.concept_id in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                    WHERE domain_id = 'DRUG'
                        and type = 'RXNORM'
                        and is_group = 0
                        and is_selectable = 1
                        and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
                )
    ) x"

echo "DRUG_EXPOSURE - ATC/RXNORM - add data into prep_concept_ancestor table"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_PCA\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT a.concept_id as ancestor_concept_id
    , COALESCE(e.concept_id, d.concept_id, c.concept_id, b.concept_id) as descendant_concept_id
    , a.is_standard
FROM (SELECT id, parent_id, concept_id, is_standard FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
           WHERE domain_id = 'DRUG' and type in ('ATC','RXNORM') and is_group = 1 and is_selectable = 1
               and id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID ) a
JOIN (SELECT id, parent_id, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'DRUG' and type in ('ATC','RXNORM')) b on a.id = b.parent_id
LEFT JOIN (SELECT id, parent_id, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'DRUG' and type in ('ATC','RXNORM')) c on b.id = c.parent_id
LEFT JOIN (SELECT id, parent_id, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'DRUG' and type in ('ATC','RXNORM')) d on c.id = d.parent_id
LEFT JOIN (SELECT id, parent_id, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'DRUG' and type in ('ATC','RXNORM')) e on d.id = e.parent_id"

echo "DRUG_EXPOSURE - ATC/RXNORM - generate parent counts"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.rollup_count = y.cnt
    , x.item_count = 0
    , x.est_count = y.cnt
FROM
    (
        SELECT ancestor_concept_id as concept_id
            , COUNT(DISTINCT person_id) cnt
        FROM
            (
                SELECT ancestor_concept_id
                    , descendant_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PCA\`
                WHERE ancestor_concept_id in
                    (
                        SELECT DISTINCT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                        WHERE domain_id = 'DRUG'
                            and type = 'ATC'
                            and is_group = 1
                            and is_selectable = 1
                            and id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID
                    )
                    and is_standard = 1
            ) a
        JOIN
            (
                SELECT d.ancestor_concept_id as concept_id, c.person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` c
                JOIN
                    (
                        SELECT ancestor_concept_id, descendant_concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\` a
                        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.ancestor_concept_id = b.concept_id
                        WHERE vocabulary_id IN  ('RxNorm', 'RxNorm Extension')
                            and concept_class_id = 'Ingredient'
                    ) d on c.drug_concept_id = d.descendant_concept_id
            ) b on a.descendant_concept_id = b.concept_id
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'DRUG'
    and type = 'ATC'
    and is_group = 1
    and id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID"

################################################
# CB_CRITERIA_ANCESTOR
################################################
echo "CB_CRITERIA_ANCESTOR - Drugs - add ingredients to drugs mapping"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBA\`
    (
          ancestor_id
        , descendant_id
    )
SELECT
      ancestor_concept_id
    , descendant_concept_id
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
WHERE ancestor_concept_id in
    (
        SELECT DISTINCT concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
        WHERE domain_id = 'DRUG'
            and type = 'RXNORM'
            and is_group = 0
            and is_selectable = 1
    )
and descendant_concept_id in
    (
        SELECT DISTINCT drug_concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`
    )"

#wait for process to end before copying
wait
## copy temp tables back to main tables, and delete temp?
cpToMain "$TBL_CBC" &
cpToMain "$TBL_PCA" &
cpToMain "$TBL_CBA" &
wait

