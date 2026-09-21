package org.pmiops.workbench.cohortbuilder;

/** SearchGroupItemQueryBuilder builds BigQuery queries for search group items. */
public final class SearchGroupItemQueryBuilder {
  // sql parts to help construct BigQuery sql statements

  public static final String CHILD_LOOKUP_SQL =
      """
             (SELECT DISTINCT c.concept_id
             FROM `${projectId}.${dataSetId}.cb_criteria` c
             JOIN (SELECT CAST(cr.id as string) AS id
                   FROM `${projectId}.${dataSetId}.cb_criteria` cr
                   WHERE concept_id IN unnest(%s)
                   AND full_text LIKE '%%_rank1]%%'
                  ) a ON (c.path LIKE CONCAT('%%.', a.id, '.%%') OR c.path LIKE CONCAT('%%.', a.id) OR c.path LIKE CONCAT(a.id, '.%%') OR c.path = a.id)
             WHERE is_standard = %s
             AND is_selectable = 1)""";
  public static final String DRUG_CHILD_LOOKUP_SQL =
      """
            (SELECT DISTINCT ca.descendant_id
            FROM `${projectId}.${dataSetId}.cb_criteria_ancestor` ca
            JOIN (SELECT DISTINCT c.concept_id
                  FROM `${projectId}.${dataSetId}.cb_criteria` c
                  JOIN (SELECT CAST(cr.id as string) AS id
                        FROM `${projectId}.${dataSetId}.cb_criteria` cr
                        WHERE concept_id IN unnest(%s)
                        AND full_text LIKE '%%_rank1]%%'
                  ) a ON (c.path LIKE CONCAT('%%.', a.id, '.%%') OR c.path LIKE CONCAT('%%.', a.id) OR c.path LIKE CONCAT(a.id, '.%%') OR c.path = a.id)
            WHERE is_standard = %s
            AND is_selectable = 1) b ON (ca.ancestor_id = b.concept_id))""";
}
