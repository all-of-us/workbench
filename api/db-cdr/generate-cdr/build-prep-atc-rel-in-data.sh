#!/bin/bash
set -e

export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

#----- RXNORM / RXNORM EXTENSION -----
# ATC4 - ATC5 --> RXNORM/RXNORM Extension ingredient
# ATC4 - ATC5 --> RXNORM/RXNORM Extension precise ingedient --> RXNORM ingredient
echo "DRUG_EXPOSURE - RXNORM - temp table - ATC4 to ATC5 to RXNORM"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
    (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
SELECT distinct e.p_concept_id, e.p_concept_code, e.p_concept_name, e.p_DOMAIN_ID,
    d.CONCEPT_ID, d.CONCEPT_CODE, d.CONCEPT_NAME, d.DOMAIN_ID
from
    (
        SELECT c1.CONCEPT_ID, c1.CONCEPT_CODE, c1.CONCEPT_NAME, c1.DOMAIN_ID, c2.CONCEPT_ID atc_5
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID--parent, rxnorm, ingredient
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID--child, atc, atc_5th
        WHERE a.RELATIONSHIP_ID IN ('RxNorm - ATC name','Mapped from', 'RxNorm - ATC')
            and c1.VOCABULARY_ID = 'RxNorm' and c1.CONCEPT_CLASS_ID = 'Ingredient' and c1.STANDARD_CONCEPT = 'S'
            and c2.VOCABULARY_ID = 'ATC' and c2.CONCEPT_CLASS_ID = 'ATC 5th' and c2.STANDARD_CONCEPT = 'C'
            and c1.concept_id in
                (
                    SELECT ANCESTOR_CONCEPT_ID
                    FROM \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
                    WHERE DESCENDANT_CONCEPT_ID in
                        (
                            SELECT distinct DRUG_CONCEPT_ID
                            FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`
                        )
                )
        UNION ALL
        SELECT c1.CONCEPT_ID, c1.CONCEPT_CODE, c1.CONCEPT_NAME, c1.DOMAIN_ID, c3.CONCEPT_ID atc_5
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID--parent, rxnorm, ingredient
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID--child, rxnorm, precise ingredient
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` b on a.CONCEPT_ID_2 = b.CONCEPT_ID_1
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on b.CONCEPT_ID_2 = c3.CONCEPT_ID--child, atc, atc_5th
        WHERE a.RELATIONSHIP_ID = 'Has form' and b.RELATIONSHIP_ID = 'RxNorm - ATC'
            and c1.VOCABULARY_ID = 'RxNorm' and c1.CONCEPT_CLASS_ID = 'Ingredient' and c1.STANDARD_CONCEPT = 'S'
            and c2.VOCABULARY_ID = 'RxNorm' and c2.CONCEPT_CLASS_ID = 'Precise Ingredient'
            and c3.VOCABULARY_ID = 'ATC' and c3.CONCEPT_CLASS_ID = 'ATC 5th' and c3.STANDARD_CONCEPT = 'C'
            and c1.concept_id in
                (
                    SELECT ANCESTOR_CONCEPT_ID
                    FROM \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
                    WHERE DESCENDANT_CONCEPT_ID in
                        (
                            SELECT distinct DRUG_CONCEPT_ID
                            FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`
                        )
                )
    ) d
left join
    (
        select c1.CONCEPT_ID as p_concept_id, c1.CONCEPT_CODE as p_concept_code, c1.CONCEPT_NAME as p_concept_name,
            c1.DOMAIN_ID as p_DOMAIN_ID, c2.CONCEPT_ID as atc_5, c2.CONCEPT_CODE, c2.CONCEPT_NAME, c2.DOMAIN_ID
        from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID --parent, atc, atc_4
        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID --child, atc, atc_5
        where RELATIONSHIP_ID = 'Subsumes'
            and c1.VOCABULARY_ID = 'ATC'
            and c1.CONCEPT_CLASS_ID = 'ATC 4th'
            and c1.STANDARD_CONCEPT = 'C'
            and c2.VOCABULARY_ID = 'ATC'
            and c2.CONCEPT_CLASS_ID = 'ATC 5th'
            and c2.STANDARD_CONCEPT = 'C'
    ) e on d.atc_5 = e.atc_5"

echo "DRUGS - temp table - ATC3 to ATC4"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
    (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
select c1.CONCEPT_ID as p_concept_id, c1.CONCEPT_CODE as p_concept_code, c1.CONCEPT_NAME as p_concept_name,
    c1.DOMAIN_ID as p_DOMAIN_ID, c2.CONCEPT_ID, c2.CONCEPT_CODE, c2.CONCEPT_NAME, c2.DOMAIN_ID
from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID
where RELATIONSHIP_ID = 'Subsumes'
    and c1.VOCABULARY_ID = 'ATC' and c1.CONCEPT_CLASS_ID = 'ATC 3rd' and c1.STANDARD_CONCEPT = 'C'
    and c2.VOCABULARY_ID = 'ATC' and c2.CONCEPT_CLASS_ID = 'ATC 4th' and c2.STANDARD_CONCEPT = 'C'
    and c2.concept_id in
        (
            select P_CONCEPT_ID
            from \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
        )
    and c2.concept_id not in
        (
            select CONCEPT_ID
            from \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
        )"

echo "DRUGS - temp table - ATC2 TO ATC3"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
    (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
select c1.CONCEPT_ID as p_concept_id, c1.CONCEPT_CODE as p_concept_code, c1.CONCEPT_NAME as p_concept_name,
    c1.DOMAIN_ID as p_DOMAIN_ID, c2.CONCEPT_ID, c2.CONCEPT_CODE, c2.CONCEPT_NAME, c2.DOMAIN_ID
from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID
where RELATIONSHIP_ID = 'Subsumes'
    and c1.VOCABULARY_ID = 'ATC' and c1.CONCEPT_CLASS_ID = 'ATC 2nd' and c1.STANDARD_CONCEPT = 'C'
    and c2.VOCABULARY_ID = 'ATC' and c2.CONCEPT_CLASS_ID = 'ATC 3rd' and c2.STANDARD_CONCEPT = 'C'
    and c2.concept_id in
        (
            select P_CONCEPT_ID
            from \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
        )
    and c2.concept_id not in
        (
            select CONCEPT_ID
            from \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
        )"

echo "DRUGS - temp table - ATC1 TO ATC2"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
    (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
select c1.CONCEPT_ID as p_concept_id, c1.CONCEPT_CODE as p_concept_code, c1.CONCEPT_NAME as p_concept_name,
    c1.DOMAIN_ID as p_DOMAIN_ID, c2.CONCEPT_ID, c2.CONCEPT_CODE, c2.CONCEPT_NAME, c2.DOMAIN_ID
from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID
where RELATIONSHIP_ID = 'Subsumes'
    and c1.VOCABULARY_ID = 'ATC' and c1.CONCEPT_CLASS_ID = 'ATC 1st' and c1.STANDARD_CONCEPT = 'C'
    and c2.VOCABULARY_ID = 'ATC' and c2.CONCEPT_CLASS_ID = 'ATC 2nd' and c2.STANDARD_CONCEPT = 'C'
    and c2.concept_id in
        (
            select P_CONCEPT_ID
            from \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
        )
    and c2.concept_id not in
        (
            select CONCEPT_ID
            from \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
        )"

