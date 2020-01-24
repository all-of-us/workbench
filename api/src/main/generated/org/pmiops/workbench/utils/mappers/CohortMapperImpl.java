package org.pmiops.workbench.utils.mappers;

import javax.annotation.Generated;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.model.Cohort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2020-01-24T11:12:16-0500",
    comments = "version: 1.3.1.Final, compiler: javac, environment: Java 1.8.0_201 (Oracle Corporation)"
)
@Component
public class CohortMapperImpl implements CohortMapper {

    @Autowired
    private UserDao userDao;

    @Override
    public DbCohort clientToDbModel(Cohort source) {
        if ( source == null ) {
            return null;
        }

        DbCohort dbCohort = new DbCohort();

        if ( source.getId() != null ) {
            dbCohort.setCohortId( source.getId() );
        }
        dbCohort.setName( source.getName() );
        dbCohort.setType( source.getType() );
        dbCohort.setDescription( source.getDescription() );
        dbCohort.setCriteria( source.getCriteria() );
        dbCohort.setCreator( userDao.findUserByUsername( source.getCreator() ) );
        dbCohort.setCreationTime( CommonMappers.timestamp( source.getCreationTime() ) );
        dbCohort.setLastModifiedTime( CommonMappers.timestamp( source.getLastModifiedTime() ) );

        return dbCohort;
    }

    @Override
    public Cohort dbModelToClient(DbCohort destination) {
        if ( destination == null ) {
            return null;
        }

        Cohort cohort = new Cohort();

        cohort.setId( destination.getCohortId() );
        cohort.setName( destination.getName() );
        cohort.setCriteria( destination.getCriteria() );
        cohort.setType( destination.getType() );
        cohort.setDescription( destination.getDescription() );
        cohort.setCreator( CommonMappers.dbUserToCreatorEmail( destination.getCreator() ) );
        cohort.setCreationTime( CommonMappers.timestamp( destination.getCreationTime() ) );
        cohort.setLastModifiedTime( CommonMappers.timestamp( destination.getLastModifiedTime() ) );

        return cohort;
    }
}
