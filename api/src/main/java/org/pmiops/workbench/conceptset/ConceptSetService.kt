package org.pmiops.workbench.conceptset

import com.google.common.annotations.VisibleForTesting
import org.pmiops.workbench.cdr.ConceptBigQueryService
import org.pmiops.workbench.db.dao.ConceptSetDao
import org.pmiops.workbench.db.model.ConceptSet
import org.pmiops.workbench.db.model.Workspace
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConceptSetService {

    // Note: Cannot use an @Autowired constructor with this version of Spring
    // Boot due to https://jira.spring.io/browse/SPR-15600. See RW-256.
    @Autowired
    private var conceptSetDao: ConceptSetDao? = null

    @Autowired
    private val conceptBigQueryService: ConceptBigQueryService? = null

    @Transactional
    fun cloneConceptSetAndConceptIds(
            conceptSet: ConceptSet, targetWorkspace: Workspace, cdrVersionChanged: Boolean): ConceptSet {
        val c = ConceptSet(conceptSet)
        if (cdrVersionChanged) {
            val omopTable = ConceptSetDao.DOMAIN_TO_TABLE_NAME[conceptSet.domainEnum]
            c.participantCount = conceptBigQueryService!!.getParticipantCountForConcepts(
                    omopTable, conceptSet.conceptIds)
        }
        c.workspaceId = targetWorkspace.workspaceId
        c.creator = targetWorkspace.creator
        c.lastModifiedTime = targetWorkspace.lastModifiedTime
        c.creationTime = targetWorkspace.creationTime
        c.version = 1
        return conceptSetDao!!.save(c)
    }

    fun getConceptSets(workspace: Workspace): List<ConceptSet> {
        // Allows for fetching concept sets for a workspace once its collection is no longer
        // bound to a session.
        return conceptSetDao!!.findByWorkspaceId(workspace.workspaceId)
    }

    @VisibleForTesting
    fun setConceptSetDao(conceptSetDao: ConceptSetDao) {
        this.conceptSetDao = conceptSetDao
    }
}
