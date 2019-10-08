package org.pmiops.workbench.tools;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.logging.Logger;
import org.pmiops.workbench.dataset.DataDictionaryEntryDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.DataDictionaryEntry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EnableJpaRepositories("org.pmiops.workbench")
@EntityScan("org.pmiops.workbench")
public class LoadDataDictionary {

  private static final Logger logger = Logger.getLogger(LoadDataDictionary.class.getName());

  @Bean
  public CommandLineRunner run(Clock clock,
      CdrVersionDao cdrVersinoDao,
      DataDictionaryEntryDao dataDictionaryEntryDao) {
    return (args) -> {
      DataDictionaryEntry entry = new DataDictionaryEntry();
      entry.setCdrVersion(cdrVersinoDao.findByIsDefault(true));
      entry.setDefinedTime(new Timestamp(clock.instant().toEpochMilli()));

      entry.setRelevantOmopTable("relevant omop table");
      entry.setFieldName("field name");
      entry.setOmopCdmStandardOrCustomField("omop cdm standard");
      entry.setDescription("desc");
      entry.setFieldType("field type");
      entry.setDataProvenance("data provenance");
      entry.setSourcePpiModule("source ppi module");
      entry.setTransformedByRegisteredTierPrivacyMethods(false);

      dataDictionaryEntryDao.save(entry);
    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(LoadDataDictionary.class).web(false).run(args);
  }
}
