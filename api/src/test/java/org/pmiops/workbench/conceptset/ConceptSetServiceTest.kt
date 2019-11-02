package org.pmiops.workbench.conceptset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

import java.util.HashSet
import java.util.stream.Collectors
import java.util.stream.Stream
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.db.dao.ConceptSetDao
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.ConceptSet
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.model.Domain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ConceptSetServiceTest {

    @Autowired
    private val conceptSetDao: ConceptSetDao? = null

    @Autowired
    private val workspaceDao: WorkspaceDao? = null

    private var conceptSetService: ConceptSetService? = null
    private var workspace: Workspace? = null

    @Before
    fun setUp() {
        conceptSetService = ConceptSetService()
        conceptSetService!!.setConceptSetDao(conceptSetDao)
        workspace = mockWorkspace()
    }

    @Test
    fun testCloneConceptSetWithNoCdrVersionChange() {
        val fromConceptSet = mockConceptSet()
        val copiedConceptSet = conceptSetService!!.cloneConceptSetAndConceptIds(fromConceptSet, workspace, false)
        assertNotNull(copiedConceptSet)
        assertEquals(copiedConceptSet.conceptIds.size.toLong(), 5)
        assertEquals(copiedConceptSet.workspaceId, workspace!!.workspaceId)
    }

    private fun mockConceptSet(): ConceptSet {
        val conceptIdsSet = Stream.of(1, 2, 3, 4, 5).collect<HashSet<Int>, Any>(Collectors.toCollection(Supplier<HashSet<Int>> { HashSet() }))

        val conceptSet = ConceptSet()
        conceptSet.conceptIds = conceptIdsSet
        conceptSet.conceptSetId = 1
        conceptSet.name = "Mock Concept Set"
        conceptSet.domainEnum = Domain.CONDITION
        return conceptSet
    }

    private fun mockWorkspace(): Workspace {
        var workspace = Workspace()
        workspace.name = "Target Workspace"
        workspace.workspaceId = 2
        workspace = workspaceDao!!.save(workspace)
        return workspace
    }
}
