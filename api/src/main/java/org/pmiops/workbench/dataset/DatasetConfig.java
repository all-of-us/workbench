package org.pmiops.workbench.dataset;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class DatasetConfig {
  public static final String DATASET_PREFIX_CODE = "DATASET_PREFIX_CODE";
  private static final int DATASET_PREFIX_DIGITS = 8;

  @Bean
  @Qualifier(DATASET_PREFIX_CODE)
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  String randomCharacters() {
    return RandomStringUtils.randomNumeric(DATASET_PREFIX_DIGITS);
  }
}
