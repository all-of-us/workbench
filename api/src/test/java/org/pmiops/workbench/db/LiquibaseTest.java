package org.pmiops.workbench.db;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class LiquibaseTest {

  List<String> whitelist = Arrays.asList("db.changelog-master.xml");

  @Test
  public void allChangeLogFilesAreIndexed() throws Exception {
    List<String> indexedChangeLogs = getIndexedChangeLogs();
    for (File f : new File("db/changelog").listFiles()) {
      if (!whitelist.contains(f.getName()) && !indexedChangeLogs.contains(f.getName())) {
        fail(f.getName() + " is in the db/changelog directory but not in db.changelog-master.xml");
      }
    }
  }

  @XmlRootElement(name = "databaseChangeLog", namespace = "http://www.liquibase.org/xml/ns/dbchangelog")
  private static class DatabaseChangeLog {
    @XmlElement(name = "include", namespace = "http://www.liquibase.org/xml/ns/dbchangelog")
    private List<Include> includes;
  }

  private static class Include {
    @XmlAttribute
    private String file;
  }

  private List<String> getIndexedChangeLogs() throws Exception {
    JAXBContext jaxbContext = JAXBContext.newInstance(DatabaseChangeLog.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    DatabaseChangeLog databaseChangeLog = (DatabaseChangeLog) unmarshaller.unmarshal(new File("db/changelog/db.changelog-master.xml"));

    return databaseChangeLog.includes.stream()
        .map(include -> include.file.split("changelog/")[1])
        .collect(Collectors.toList());
  }
}
