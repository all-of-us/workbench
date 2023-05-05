package org.pmiops.workbench.config;

import java.util.Optional;
import org.springframework.context.annotation.Configuration;

@Configuration // provide as an injectible dependency
// This class is just a proxy for retrieving environment variables that can be overriden in tests.
public class EnvVars {
  public Optional<String> get(String name) {
    return Optional.ofNullable(System.getenv(name)).filter(s -> !s.isBlank());
  }
}
