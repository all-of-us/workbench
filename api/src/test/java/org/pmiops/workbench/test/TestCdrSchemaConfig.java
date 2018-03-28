package org.pmiops.workbench.test;

import com.google.gson.Gson;
import java.io.FileReader;
import java.io.IOException;
import org.pmiops.workbench.config.CdrSchemaConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestCdrSchemaConfig {

  @Bean
  CdrSchemaConfig provideCdrSchemaConfig() throws IOException {
    Gson gson = new Gson();
    return gson.fromJson(new FileReader("config/cdm/cdm_5_2.json"),
          CdrSchemaConfig.class);
  }
}
