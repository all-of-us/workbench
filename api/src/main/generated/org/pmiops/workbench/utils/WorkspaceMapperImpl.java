package org.pmiops.workbench.utils;

import java.util.HashSet;
import java.util.List;
import javax.annotation.Generated;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2020-01-23T15:35:11-0500",
    comments = "version: 1.3.1.Final, compiler: javac, environment: Java 1.8.0_201 (Oracle Corporation)"
)
@Component
public class WorkspaceMapperImpl implements WorkspaceMapper {

    @Override
    public UserRole userToUserRole(DbUser user) {
        if ( user == null ) {
            return null;
        }

        UserRole userRole = new UserRole();

        userRole.setEmail( user.getUsername() );
        userRole.setRole( roleToWorkspaceAccessLevel( user.getDataAccessLevel() ) );
        userRole.setGivenName( user.getGivenName() );
        userRole.setFamilyName( user.getFamilyName() );

        return userRole;
    }

    @Override
    public Workspace toApiWorkspace(DbWorkspace dbWorkspace, FirecloudWorkspace fcWorkspace) {
        if ( dbWorkspace == null && fcWorkspace == null ) {
            return null;
        }

        Workspace workspace = new Workspace();

        if ( dbWorkspace != null ) {
            workspace.setCdrVersionId( CommonMappers.cdrVersionToId( dbWorkspace.getCdrVersion() ) );
            workspace.setName( dbWorkspace.getName() );
            workspace.setResearchPurpose( workspaceToResearchPurpose( dbWorkspace ) );
            workspace.setEtag( cdrVersionToEtag( dbWorkspace.getVersion() ) );
            workspace.setDataAccessLevel( dbWorkspace.getDataAccessLevelEnum() );
            workspace.setCreationTime( CommonMappers.timestamp( dbWorkspace.getCreationTime() ) );
            workspace.setLastModifiedTime( CommonMappers.timestamp( dbWorkspace.getLastModifiedTime() ) );
            workspace.setPublished( dbWorkspace.getPublished() );
        }
        if ( fcWorkspace != null ) {
            workspace.setCreator( fcWorkspace.getCreatedBy() );
            workspace.setGoogleBucketName( fcWorkspace.getBucketName() );
            workspace.setId( fcWorkspace.getName() );
            workspace.setNamespace( fcWorkspace.getNamespace() );
        }

        return workspace;
    }

    @Override
    public void mergeResearchPurposeIntoWorkspace(DbWorkspace dbWorkspace, ResearchPurpose researchPurpose) {
        if ( researchPurpose == null ) {
            return;
        }

        if ( dbWorkspace.getSpecificPopulationsEnum() != null ) {
            List<SpecificPopulationEnum> list = researchPurpose.getPopulationDetails();
            if ( list != null ) {
                dbWorkspace.getSpecificPopulationsEnum().clear();
                dbWorkspace.getSpecificPopulationsEnum().addAll( list );
            }
            else {
                dbWorkspace.setSpecificPopulationsEnum( null );
            }
        }
        else {
            List<SpecificPopulationEnum> list = researchPurpose.getPopulationDetails();
            if ( list != null ) {
                dbWorkspace.setSpecificPopulationsEnum( new HashSet<SpecificPopulationEnum>( list ) );
            }
        }

        afterResearchPurposeIntoWorkspace( dbWorkspace, researchPurpose );
    }

    @Override
    public ResearchPurpose workspaceToResearchPurpose(DbWorkspace dbWorkspace) {
        if ( dbWorkspace == null ) {
            return null;
        }

        ResearchPurpose researchPurpose = new ResearchPurpose();

        researchPurpose.setAdditionalNotes( dbWorkspace.getAdditionalNotes() );
        researchPurpose.setApproved( dbWorkspace.getApproved() );
        researchPurpose.setAncestry( dbWorkspace.getAncestry() );
        researchPurpose.setAnticipatedFindings( dbWorkspace.getAnticipatedFindings() );
        researchPurpose.setCommercialPurpose( dbWorkspace.getCommercialPurpose() );
        researchPurpose.setControlSet( dbWorkspace.getControlSet() );
        researchPurpose.setDiseaseFocusedResearch( dbWorkspace.getDiseaseFocusedResearch() );
        researchPurpose.setDiseaseOfFocus( dbWorkspace.getDiseaseOfFocus() );
        researchPurpose.setDrugDevelopment( dbWorkspace.getDrugDevelopment() );
        researchPurpose.setEducational( dbWorkspace.getEducational() );
        researchPurpose.setIntendedStudy( dbWorkspace.getIntendedStudy() );
        researchPurpose.setMethodsDevelopment( dbWorkspace.getMethodsDevelopment() );
        researchPurpose.setOtherPopulationDetails( dbWorkspace.getOtherPopulationDetails() );
        researchPurpose.setOtherPurpose( dbWorkspace.getOtherPurpose() );
        researchPurpose.setOtherPurposeDetails( dbWorkspace.getOtherPurposeDetails() );
        researchPurpose.setPopulation( dbWorkspace.getPopulation() );
        researchPurpose.setPopulationDetails( ordinalsToSpecificPopulationEnumList( dbWorkspace.getPopulationDetails() ) );
        researchPurpose.setPopulationHealth( dbWorkspace.getPopulationHealth() );
        researchPurpose.setReasonForAllOfUs( dbWorkspace.getReasonForAllOfUs() );
        researchPurpose.setReviewRequested( dbWorkspace.getReviewRequested() );
        researchPurpose.setSocialBehavioral( dbWorkspace.getSocialBehavioral() );
        researchPurpose.setTimeRequested( CommonMappers.timestamp( dbWorkspace.getTimeRequested() ) );

        afterWorkspaceIntoResearchPurpose( researchPurpose, dbWorkspace );

        return researchPurpose;
    }
}
