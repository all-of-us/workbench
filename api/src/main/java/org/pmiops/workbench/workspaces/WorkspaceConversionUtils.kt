package org.pmiops.workbench.workspaces

import java.sql.Timestamp
import java.util.ArrayList
import java.util.HashSet
import java.util.stream.Collectors
import org.pmiops.workbench.api.Etags
import org.pmiops.workbench.db.model.UserRecentWorkspace
import org.pmiops.workbench.db.model.Workspace.FirecloudWorkspaceId
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry
import org.pmiops.workbench.model.RecentWorkspace
import org.pmiops.workbench.model.ResearchPurpose
import org.pmiops.workbench.model.UserRole
import org.pmiops.workbench.model.Workspace
import org.pmiops.workbench.model.WorkspaceAccessLevel

object WorkspaceConversionUtils {

    fun toApiWorkspaceAccessLevel(firecloudAccessLevel: String): WorkspaceAccessLevel {
        return if (firecloudAccessLevel == WorkspaceService.PROJECT_OWNER_ACCESS_LEVEL) {
            WorkspaceAccessLevel.OWNER
        } else {
            WorkspaceAccessLevel.fromValue(firecloudAccessLevel)
        }
    }

    fun toApiWorkspace(workspace: org.pmiops.workbench.db.model.Workspace): Workspace {
        val researchPurpose = createResearchPurpose(workspace)
        val workspaceId = workspace.firecloudWorkspaceId

        val result = Workspace()
                .etag(Etags.fromVersion(workspace.version))
                .lastModifiedTime(workspace.lastModifiedTime!!.time)
                .creationTime(workspace.creationTime!!.time)
                .dataAccessLevel(workspace.dataAccessLevelEnum)
                .name(workspace.name)
                .id(workspaceId.workspaceName)
                .namespace(workspaceId.workspaceNamespace)
                .published(workspace.published)
                .researchPurpose(researchPurpose)
        if (workspace.creator != null) {
            result.setCreator(workspace.creator!!.email)
        }
        if (workspace.cdrVersion != null) {
            result.setCdrVersionId(workspace.cdrVersion!!.cdrVersionId.toString())
        }

        return result
    }

    fun toApiWorkspace(
            workspace: org.pmiops.workbench.db.model.Workspace,
            fcWorkspace: org.pmiops.workbench.firecloud.model.Workspace): Workspace {
        val researchPurpose = createResearchPurpose(workspace)
        if (workspace.population) {
            researchPurpose.setPopulationDetails(ArrayList(workspace.specificPopulationsEnum!!))
        }

        val result = Workspace()
                .etag(Etags.fromVersion(workspace.version))
                .lastModifiedTime(workspace.lastModifiedTime!!.time)
                .creationTime(workspace.creationTime!!.time)
                .dataAccessLevel(workspace.dataAccessLevelEnum)
                .name(workspace.name)
                .id(fcWorkspace.getName())
                .namespace(fcWorkspace.getNamespace())
                .researchPurpose(researchPurpose)
                .published(workspace.published)
                .googleBucketName(fcWorkspace.getBucketName())
        if (fcWorkspace.getCreatedBy() != null) {
            result.setCreator(fcWorkspace.getCreatedBy())
        }
        if (workspace.cdrVersion != null) {
            result.setCdrVersionId(workspace.cdrVersion!!.cdrVersionId.toString())
        }

        return result
    }

    fun toDbWorkspace(workspace: Workspace): org.pmiops.workbench.db.model.Workspace {
        val result = org.pmiops.workbench.db.model.Workspace()

        if (workspace.getDataAccessLevel() != null) {
            result.dataAccessLevelEnum = workspace.getDataAccessLevel()
        }

        result.name = workspace.getName()

        if (workspace.getResearchPurpose() != null) {
            setResearchPurposeDetails(result, workspace.getResearchPurpose())
            result.reviewRequested = workspace.getResearchPurpose().getReviewRequested()
            if (workspace.getResearchPurpose().getTimeRequested() != null) {
                result.timeRequested = Timestamp(workspace.getResearchPurpose().getTimeRequested())
            }
            result.approved = workspace.getResearchPurpose().getApproved()
        }

        return result
    }

    fun toApiUserRole(
            user: org.pmiops.workbench.db.model.User, aclEntry: WorkspaceAccessEntry): UserRole {
        val result = UserRole()
        result.setEmail(user.email)
        result.setGivenName(user.givenName)
        result.setFamilyName(user.familyName)
        result.setRole(WorkspaceAccessLevel.fromValue(aclEntry.getAccessLevel()))
        return result
    }

    /**
     * This probably doesn't belong in a mapper service but it makes the refactoring easier atm. Sets
     * user-editable research purpose detail fields.
     */
    fun setResearchPurposeDetails(
            dbWorkspace: org.pmiops.workbench.db.model.Workspace, purpose: ResearchPurpose) {
        dbWorkspace.diseaseFocusedResearch = purpose.getDiseaseFocusedResearch()
        dbWorkspace.diseaseOfFocus = purpose.getDiseaseOfFocus()
        dbWorkspace.methodsDevelopment = purpose.getMethodsDevelopment()
        dbWorkspace.controlSet = purpose.getControlSet()
        dbWorkspace.ancestry = purpose.getAncestry()
        dbWorkspace.commercialPurpose = purpose.getCommercialPurpose()
        dbWorkspace.population = purpose.getPopulation()
        if (purpose.getPopulation()) {
            dbWorkspace.specificPopulationsEnum = HashSet(purpose.getPopulationDetails())
        }
        dbWorkspace.socialBehavioral = purpose.getSocialBehavioral()
        dbWorkspace.populationHealth = purpose.getPopulationHealth()
        dbWorkspace.educational = purpose.getEducational()
        dbWorkspace.drugDevelopment = purpose.getDrugDevelopment()
        dbWorkspace.otherPurpose = purpose.getOtherPurpose()
        dbWorkspace.otherPurposeDetails = purpose.getOtherPurposeDetails()
        dbWorkspace.additionalNotes = purpose.getAdditionalNotes()
        dbWorkspace.reasonForAllOfUs = purpose.getReasonForAllOfUs()
        dbWorkspace.intendedStudy = purpose.getIntendedStudy()
        dbWorkspace.anticipatedFindings = purpose.getAnticipatedFindings()
        dbWorkspace.otherPopulationDetails = purpose.getOtherPopulationDetails()
    }

    private fun createResearchPurpose(
            workspace: org.pmiops.workbench.db.model.Workspace): ResearchPurpose {
        val researchPurpose = ResearchPurpose()
                .diseaseFocusedResearch(workspace.diseaseFocusedResearch)
                .diseaseOfFocus(workspace.diseaseOfFocus)
                .methodsDevelopment(workspace.methodsDevelopment)
                .controlSet(workspace.controlSet)
                .ancestry(workspace.ancestry)
                .commercialPurpose(workspace.commercialPurpose)
                .socialBehavioral(workspace.socialBehavioral)
                .educational(workspace.educational)
                .drugDevelopment(workspace.drugDevelopment)
                .populationHealth(workspace.populationHealth)
                .otherPurpose(workspace.otherPurpose)
                .otherPurposeDetails(workspace.otherPurposeDetails)
                .population(workspace.population)
                .reasonForAllOfUs(workspace.reasonForAllOfUs)
                .intendedStudy(workspace.intendedStudy)
                .anticipatedFindings(workspace.anticipatedFindings)
                .additionalNotes(workspace.additionalNotes)
                .reviewRequested(workspace.reviewRequested)
                .approved(workspace.approved)
                .otherPopulationDetails(workspace.otherPopulationDetails)
        if (workspace.timeRequested != null) {
            researchPurpose.timeRequested(workspace.timeRequested!!.time)
        }
        return researchPurpose
    }

    fun buildRecentWorkspace(
            userRecentWorkspace: UserRecentWorkspace,
            dbWorkspace: org.pmiops.workbench.db.model.Workspace,
            accessLevel: WorkspaceAccessLevel): RecentWorkspace {
        return RecentWorkspace()
                .workspace(toApiWorkspace(dbWorkspace))
                .accessedTime(userRecentWorkspace.lastAccessDate!!.toString())
                .accessLevel(accessLevel)
    }

    fun buildRecentWorkspaceList(
            userRecentWorkspaces: List<UserRecentWorkspace>,
            dbWorkspacesByWorkspaceId: Map<Long, org.pmiops.workbench.db.model.Workspace>,
            workspaceAccessLevelsByWorkspaceId: Map<Long, WorkspaceAccessLevel>): List<RecentWorkspace> {
        return userRecentWorkspaces.stream()
                .map<Any> { userRecentWorkspace ->
                    buildRecentWorkspace(
                            userRecentWorkspace,
                            dbWorkspacesByWorkspaceId[userRecentWorkspace.workspaceId],
                            workspaceAccessLevelsByWorkspaceId[userRecentWorkspace.workspaceId])
                }
                .collect<List<RecentWorkspace>, Any>(Collectors.toList())
    }
}
