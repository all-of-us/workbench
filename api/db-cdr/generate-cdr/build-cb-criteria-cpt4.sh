#!/bin/bash
# do not output cmd-line for now
set -e

TBL_CBC='cb_criteria'
TBL_CBA='cb_criteria_ancestor'
TBL_ANC='prep_cpt_ancestor'

export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

echo "Creating CPT4 hierarchy"

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
source ./generate-cdr/cb-criteria-utils.sh
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
echo "Creating temp table for $TBL_CBA"
TBL_CBA=$(createTmpTable $TBL_CBA)
echo "Creating temp table for $TBL_ANC"
TBL_ANC=$(createTmpTable $TBL_ANC)
####### end common block ###########

CB_CRITERIA_START_ID=1000000000
CB_CRITERIA_END_ID=2000000000

echo "CPT4 - SOURCE - insert root"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
        , concept_id
        , code
        , name
        , rollup_count
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
     (a.id + $CB_CRITERIA_START_ID ) id
    , CASE WHEN a.parent_id=0 THEN 0 ELSE a.parent_id + $CB_CRITERIA_START_ID END as parent_id
    , a.domain_id
    , a.is_standard
    , a.type
    , a.subtype
    , a.concept_id
    , a.code
    , CASE WHEN b.concept_id is not null THEN b.concept_name ELSE a.name END AS name
    , CASE WHEN a.parent_id != $CB_CRITERIA_START_ID THEN 0 ELSE null END AS rollup_count
    , CASE
        WHEN a.parent_id != $CB_CRITERIA_START_ID THEN
            CASE
                WHEN c.cnt is null THEN 0
                ELSE c.cnt
            END
        ELSE null
      END AS item_count
    , CASE WHEN a.is_group = 0 and a.is_selectable = 1 THEN c.cnt ELSE null END AS est_count
    , a.is_group
    , a.is_selectable
    , a.has_attribute
    , a.has_hierarchy
    , CAST((a.id + $CB_CRITERIA_START_ID) as STRING) as path
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_cpt\` a
LEFT JOIN
    (
        SELECT concept_id, concept_name
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
        WHERE vocabulary_id = 'CPT4'
    ) b on a.concept_id = b.concept_id
LEFT JOIN
    (
        -- get the count of distinct patients coded with each concept
        SELECT concept_id, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id in
                (
                    -- get all concepts that are selectable
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_cpt\`
                    WHERE type = 'CPT4'
                        and is_selectable = 1
                )
        GROUP BY 1
    ) c on b.concept_id = c.concept_id
WHERE a.type = 'CPT4'
    AND
        (
            -- get all groups and get all children that have a count
            is_group = 1
            OR
            (
                is_group = 0
                AND is_selectable = 1
                AND
                    (
                        c.cnt != 0
                        OR c.cnt is not null
                    )
            )
      )
    AND a.parent_id = 0
ORDER BY 1"

# for each loop, add all items (children/parents) directly under the items that were previously added
# currently, there are only 7 levels, but we run it 8 times to be safe
# if this number is changed, you will need to change the number of JOINS in the query below for prep_cpt_ancestor
for i in {1..8};
do
  echo "CPT4 - SOURCE - add level $i"
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
      (
            id
          , parent_id
          , domain_id
          , is_standard
          , type
          , subtype
          , concept_id
          , code
          , name
          , rollup_count
          , item_count
          , est_count
          , is_group
          , is_selectable
          , has_attribute
          , has_hierarchy
          , path
      )
  SELECT
       (a.id + $CB_CRITERIA_START_ID ) id
      , CASE WHEN a.parent_id=0 THEN 0 ELSE a.parent_id + $CB_CRITERIA_START_ID END as parent_id
      , a.domain_id
      , a.is_standard
      , a.type
      , a.subtype
      , a.concept_id
      , a.code
      , CASE WHEN b.concept_id is not null THEN b.concept_name ELSE a.name END AS name
      , CASE WHEN a.parent_id != $CB_CRITERIA_START_ID THEN 0 ELSE null END AS rollup_count
      , CASE
          WHEN a.parent_id != $CB_CRITERIA_START_ID THEN
              CASE
                  WHEN c.cnt is null THEN 0
                  ELSE c.cnt
              END
          ELSE null
        END AS item_count
      , CASE WHEN a.is_group = 0 and a.is_selectable = 1 THEN c.cnt ELSE null END AS est_count
      , a.is_group
      , a.is_selectable
      , a.has_attribute
      , a.has_hierarchy
      , (SELECT CONCAT(path, '.', (a.id + $CB_CRITERIA_START_ID )) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id = (a.parent_id + $CB_CRITERIA_START_ID)) AS path
  FROM \`$BQ_PROJECT.$BQ_DATASET.prep_cpt\` a
  LEFT JOIN
      (
          SELECT concept_id, concept_name
          FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
          WHERE vocabulary_id = 'CPT4'
      ) b on a.concept_id = b.concept_id
  LEFT JOIN
      (
          -- get the count of distinct patients coded with each concept
          SELECT concept_id, COUNT(DISTINCT person_id) cnt
          FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
          WHERE is_standard = 0
              and concept_id in
                  (
                      -- get all concepts that are selectable
                      SELECT concept_id
                      FROM \`$BQ_PROJECT.$BQ_DATASET.prep_cpt\`
                      WHERE type = 'CPT4'
                          and is_selectable = 1
                  )
          GROUP BY 1
      ) c on b.concept_id = c.concept_id
  WHERE a.type = 'CPT4'
      AND
          (
              -- get all groups and get all children that have a count
              is_group = 1
              OR
              (
                  is_group = 0
                  AND is_selectable = 1
                  AND
                      (
                          c.cnt != 0
                          OR c.cnt is not null
                      )
              )
        )
      AND a.id in (SELECT id
          FROM \`$BQ_PROJECT.$BQ_DATASET.prep_cpt\` where LENGTH(path) - LENGTH(REPLACE(path, '.', '')) = $i)
  ORDER BY 1"
done

############ prep_cpt_ancestor ############
echo "CPT4 - SOURCE - add ancestor data"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_ANC\`
    (
          ancestor_id
        , descendant_id
    )
SELECT
      DISTINCT a.id ancestor_id
    , coalesce(h.id, g.id, f.id, e.id, d.id, c.id, b.id) descendant_id
FROM (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
           WHERE domain_id = 'PROCEDURE' and type = 'CPT4' and is_standard = 0 and is_group = 1
                 and parent_id !=$CB_CRITERIA_START_ID
                 and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) a
JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) b on a.id  = b.parent_id
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) c on b.id  = c.parent_id
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) d on c.id  = d.parent_id
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) e on d.id  = e.parent_id
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) f on e.id  = f.parent_id
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) g on f.id  = g.parent_id
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) h on g.id  = h.parent_id"

############ cb_criteria - update counts ############
echo "CPT4 - SOURCE - generate parent counts"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT e.id, COUNT(DISTINCT f.person_id) cnt
        FROM
            (
                -- for each group, get it and all items under it
                SELECT a.id, b.descendant_id
                FROM
                    (
                        -- get all groups except the top level
                        SELECT id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                        WHERE type = 'CPT4'
                            and parent_id != $CB_CRITERIA_START_ID
                            and is_group = 1
                            and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
                    ) a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.$TBL_ANC\` b on a.id = b.ancestor_id
            ) e
        LEFT JOIN
            (
                SELECT c.id, d.person_id, d.concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` c
                JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` d on c.concept_id = d.concept_id
                WHERE c.type = 'CPT4'
                    and c.is_selectable = 1
                    and d.is_standard = 0
                    and c.id > $CB_CRITERIA_START_ID and c.id < $CB_CRITERIA_END_ID
            ) f on e.descendant_id = f.id
        GROUP BY 1
    ) y
WHERE x.id = y.id
      and x.id > $CB_CRITERIA_START_ID and x.id < $CB_CRITERIA_END_ID"

echo "CPT4 - SOURCE - delete zero count parents"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"DELETE
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
WHERE type = 'CPT4'
    and is_group = 1
    and
        (
            (parent_id !=$CB_CRITERIA_START_ID and rollup_count = 0)
            or id not in
                (
                    SELECT parent_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                    WHERE type = 'CPT4'
                      and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
                )
        )"

echo "CPT4 - SOURCE - update CPT4 domain"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.domain_id = y.domain_id
FROM (
  SELECT c.concept_id, UPPER(c.domain_id) as domain_id
  FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` cr
  JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (cr.concept_id = c.concept_id and cr.type = c.vocabulary_id)
  AND cr.type = 'CPT4'
  AND cr.is_standard = 0
  AND c.domain_id in ('Observation', 'Measurement', 'Device', 'Drug')
) y
WHERE x.concept_id = y.concept_id
AND x.type = 'CPT4'
AND x.is_standard = 0"

################################################
# CB_CRITERIA_ANCESTOR
################################################
echo "CB_CRITERIA_ANCESTOR - Drugs - add any drugs from the CPT4 hierarchy"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBA\`
    (
          ancestor_id
        , descendant_id
    )
SELECT concept_id as ancestor_concept_id , concept_id as descendant_concept_id
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
WHERE domain_id = 'DRUG'
and type in ('CPT4')
and is_standard = 0
and is_group = 0
and is_selectable = 1"

## wait for process to end before copying
wait
## copy tmp tables back to main tables and delete tmp
cpToMainAndDeleteTmp "$TBL_CBC" "$TBL_CBA" "$TBL_ANC"
