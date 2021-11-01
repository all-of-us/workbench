#!/bin/bash
set -e

# make-bq-prep-icd10-rel-cm-src-tables.sh
#1719 - #1815 : prep_icd10_rel_cm_src : make-bq-criteria-tables.sh
#     	Uses tables: prep_concept_merged, prep_concept_relationship_merged,
#	                   concept, cb_search_all_events
echo "ICD10CM - SOURCE - create prep_icd10_rel_cm_src"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\` AS
SELECT * EXCEPT(rnk)
FROM
    (
        SELECT DISTINCT
              c1.concept_id AS p_concept_id
            , c1.concept_code AS p_concept_code
            , c1.concept_name AS p_concept_name
            , c2.concept_id
            , c2.concept_code
            , c2.concept_name
            , RANK() OVER (PARTITION BY c2.concept_code ORDER BY LENGTH(c1.concept_code) DESC) rnk
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_relationship_merged\` cr
        JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`c1 ON cr.concept_id_1 = c1.concept_id
        JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`c2 ON cr.concept_id_2 = c2.concept_id
        WHERE c1.vocabulary_id ='ICD10CM'
            AND c2.vocabulary_id ='ICD10CM'
            AND cr.relationship_id = 'Subsumes'
    )
WHERE rnk =1"

# adding in child items that fell out due to not having a relationship to a parent in concept_relationship
# from the joins below we are going through parents, grandparents, great grandparents to find the first parent that exists
echo "ICD10CM - SOURCE - adding extra child items to prep_icd10_rel_cm_src"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\`
    (
          p_concept_id
        , p_concept_code
        , p_concept_name
        , concept_id
        , concept_code
        , concept_name
    )
SELECT
      COALESCE(d.concept_id, e.concept_id, f.concept_id) as p_concept_id
    , COALESCE(d.concept_code, e.concept_code, f.concept_code) as p_concept_code
    , COALESCE(d.concept_name, e.concept_name, f.concept_name) as p_concept_name
    , c.concept_id
    , c.concept_code
    , c.concept_name
FROM
    (
        SELECT DISTINCT
              a.concept_id
            , b.concept_code
            , b.vocabulary_id
            , b.concept_name
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.concept_id = b.concept_id
        WHERE a.is_standard = 0
            and b.vocabulary_id = 'ICD10CM'
            and a.concept_id not in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\`
            )
    ) c
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on ( TRIM(LEFT(c.concept_code, LENGTH(c.concept_code)-1), '.') = d.concept_code and c.vocabulary_id = d.vocabulary_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on ( TRIM(LEFT(c.concept_code, LENGTH(c.concept_code)-2), '.') = e.concept_code and c.vocabulary_id = e.vocabulary_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` f on ( TRIM(LEFT(c.concept_code, LENGTH(c.concept_code)-3), '.') = f.concept_code and c.vocabulary_id = f.vocabulary_id)"

# adding in parent items that fell out due to not having a relationship in concept_relationship
for i in {1..2};
do
  echo "ICD10CM - SOURCE - adding extra parent items to prep_icd10_rel_cm_src"
  bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\`
      (
            p_concept_id
          , p_concept_code
          , p_concept_name
          , concept_id
          , concept_code
          , concept_name
      )
  SELECT DISTINCT
        b.concept_id as p_concept_id
      , b.concept_code as p_concept_name
      , b.concept_name as p_concept_name
      , a.p_concept_id as concept_id
      , a.p_concept_code as concept_code
      , a.p_concept_name as concept_name
  FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\` a
  JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on ( TRIM(LEFT(a.p_concept_code, LENGTH(a.p_concept_code)-1), '.') = b.concept_code and b.vocabulary_id = 'ICD10CM' )
  WHERE a.p_concept_id NOT IN
      (
          SELECT DISTINCT concept_id
          FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\`
      )
      and a.p_concept_code is not null"
done

#1817 - #1868 : prep_icd10_rel_src_in_data : make-bq-criteria-tables.sh
#        Uses tables: cb_search_all_events, prep_concept_merged, prep_icd10_rel_cm_src
echo "ICD10CM - SOURCE - temp table inserting level 0"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_src_in_data\`
    (
        p_concept_id    INT64,
        p_concept_code  STRING,
        p_concept_name  STRING,
        concept_id      INT64,
        concept_code    STRING,
        concept_name    STRING
    )
AS SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\`
WHERE concept_id in
    (
        SELECT DISTINCT a.concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\` b on a.concept_id = b.concept_id
        WHERE a.is_standard = 0
            and b.vocabulary_id = 'ICD10CM'
    )"

# for each loop, add all items (children/parents) related to the items that were previously added
# we loop one more time that is actually needed
for i in {1..5};
do
    echo "ICD10CM - SOURCE - temp table inserting level $i"
    bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_src_in_data\`
        (
              p_concept_id
            , p_concept_code
            , p_concept_name
            , concept_id
            , concept_code
            , concept_name
        )
    SELECT *
    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\`
    WHERE
        concept_id in
            (
                SELECT p_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_src_in_data\`
            )
        and concept_id not in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_src_in_data\`
            )"
done

