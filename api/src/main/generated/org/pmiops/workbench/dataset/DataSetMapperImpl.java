package org.pmiops.workbench.dataset;

import javax.annotation.Generated;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbDataDictionaryEntry;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2020-01-23T15:35:11-0500",
    comments = "version: 1.3.1.Final, compiler: javac, environment: Java 1.8.0_201 (Oracle Corporation)"
)
@Component
public class DataSetMapperImpl implements DataSetMapper {

    @Override
    public DataDictionaryEntry toApi(DbDataDictionaryEntry dbModel) {
        if ( dbModel == null ) {
            return null;
        }

        DataDictionaryEntry dataDictionaryEntry = new DataDictionaryEntry();

        dataDictionaryEntry.setCdrVersionId( dbModelCdrVersionCdrVersionId( dbModel ) );
        dataDictionaryEntry.setDefinedTime( CommonMappers.timestamp( dbModel.getDefinedTime() ) );
        dataDictionaryEntry.setRelevantOmopTable( dbModel.getRelevantOmopTable() );
        dataDictionaryEntry.setFieldName( dbModel.getFieldName() );
        dataDictionaryEntry.setOmopCdmStandardOrCustomField( dbModel.getOmopCdmStandardOrCustomField() );
        dataDictionaryEntry.setDescription( dbModel.getDescription() );
        dataDictionaryEntry.setFieldType( dbModel.getFieldType() );
        dataDictionaryEntry.setDataProvenance( dbModel.getDataProvenance() );
        dataDictionaryEntry.setSourcePpiModule( dbModel.getSourcePpiModule() );
        dataDictionaryEntry.setTransformedByRegisteredTierPrivacyMethods( dbModel.getTransformedByRegisteredTierPrivacyMethods() );

        return dataDictionaryEntry;
    }

    private Long dbModelCdrVersionCdrVersionId(DbDataDictionaryEntry dbDataDictionaryEntry) {
        if ( dbDataDictionaryEntry == null ) {
            return null;
        }
        DbCdrVersion cdrVersion = dbDataDictionaryEntry.getCdrVersion();
        if ( cdrVersion == null ) {
            return null;
        }
        long cdrVersionId = cdrVersion.getCdrVersionId();
        return cdrVersionId;
    }
}
