package org.pmiops.workbench.tools;

import org.pmiops.workbench.db.dao.ConfigDao;
import org.pmiops.workbench.db.model.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;

@SpringBootApplication
@EnableJpaRepositories("org.pmiops.workbench.db.dao")
@EntityScan("org.pmiops.workbench.db.model")
public class ConfigLoader {

  @Bean
  public CommandLineRunner run(ConfigDao configDao) {
    return (args) -> {
      throw new Exception("Config = " + configDao.findOne(Config.MAIN_CONFIG_ID));
    };
  }
  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(ConfigLoader.class).web(false).run(args);
  }

}
