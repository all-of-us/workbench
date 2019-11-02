package org.pmiops.workbench.db.dao

import com.google.common.collect.ImmutableMap
import org.pmiops.workbench.db.model.ConceptSet
import org.pmiops.workbench.model.Domain
import org.springframework.data.repository.CrudRepository

interface ConceptSetDao : CrudRepository<ConceptSet, Long> {

    fun findByWorkspaceId(workspaceId: Long): List<ConceptSet>

    fun findByWorkspaceIdAndSurvey(workspaceId: Long, surveyId: Short): List<ConceptSet>

    /** Returns the concept set in the workspace with the specified name, or null if there is none.  */
    fun findConceptSetByNameAndWorkspaceId(name: String, workspaceId: Long): ConceptSet

    fun findAllByConceptSetIdIn(conceptSetIds: Collection<Long>): List<ConceptSet>

    companion object {

        // TODO: consider putting this in CDM config, fetching it from there
        val DOMAIN_TO_TABLE_NAME = ImmutableMap.builder<Domain, String>()
                .put(Domain.CONDITION, "condition_occurrence")
                .put(Domain.DEATH, "death")
                .put(Domain.DEVICE, "device_exposure")
                .put(Domain.DRUG, "drug_exposure")
                .put(Domain.MEASUREMENT, "measurement")
                .put(Domain.OBSERVATION, "observation")
                .put(Domain.PROCEDURE, "procedure_occurrence")
                .put(Domain.PERSON, "person")
                .put(Domain.VISIT, "visit_occurrence")
                .put(Domain.SURVEY, "observation")
                .build()
    }
}
