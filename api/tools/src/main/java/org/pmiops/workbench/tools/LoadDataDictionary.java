package org.pmiops.workbench.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.DataDictionaryEntryDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.DataDictionaryEntry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("org.pmiops.workbench")
@EntityScan("org.pmiops.workbench")
public class LoadDataDictionary {

  private static final Logger logger = Logger.getLogger(LoadDataDictionary.class.getName());

  private Resource[] getDataDictionaryExportFiles() throws IOException {
    ClassLoader cl = this.getClass().getClassLoader();
    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
    return resolver.getResources("classpath*:/data_dictionary_exports/*.yaml");
  }

  @Bean
  public CommandLineRunner run(
      CdrVersionDao cdrVersionDao, DataDictionaryEntryDao dataDictionaryEntryDao) {
    return (args) -> {
      CdrVersion defaultCdrVersion = cdrVersionDao.findByIsDefault(true);

      for (Resource resource : getDataDictionaryExportFiles()) {
        InputStream is = resource.getInputStream();
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        mapper.setDateFormat(df);

        mapper.registerModule(new KotlinModule());
        DataDictionary dd = mapper.readValue(is, DataDictionary.class);

        Timestamp newEntryDefinedTime = dd.getMeta_data()[0].getCreated_time();

        CdrVersion cdrVersion = cdrVersionDao.findByName(dd.getMeta_data()[0].getCdr_version());

        if (cdrVersion == null) {
          // Skip over Data Dictionaries for CDR Versions not in the current environment
          continue;
        }

        for (AvailableField field : dd.getTransformations()[0].getAvailable_fields()) {
          DataDictionaryEntry entry =
              dataDictionaryEntryDao.findByRelevantOmopTableAndFieldNameAndCdrVersion(
                  field.getRelevant_omop_table(), field.getField_name(), cdrVersion);

          // We are skipping ahead if the defined times match by assuming that the definition has
          // not changed.
          if (entry != null && newEntryDefinedTime.before(entry.getDefinedTime())) {
            continue;
          }

          if (entry == null) {
            entry = new DataDictionaryEntry();
            entry.setRelevantOmopTable(field.getRelevant_omop_table());
            entry.setFieldName(field.getField_name());
            entry.setCdrVersion(defaultCdrVersion);
          }

          entry.setDefinedTime(newEntryDefinedTime);
          entry.setOmopCdmStandardOrCustomField(field.getOmop_cdm_standard_or_custom_field());
          entry.setDescription(field.getDescription());
          entry.setFieldType(field.getField_type());
          entry.setDataProvenance(field.getData_provenance());
          entry.setSourcePpiModule(field.getSource_ppi_module());
          entry.setTransformedByRegisteredTierPrivacyMethods(
              field.getTransformed_by_registered_tier_privacy_methods());

          dataDictionaryEntryDao.save(entry);
        }
      }
    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(LoadDataDictionary.class).web(false).run(args);
  }
}
