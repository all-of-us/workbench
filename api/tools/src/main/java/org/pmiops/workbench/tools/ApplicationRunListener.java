package org.pmiops.workbench.tools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * A custom application listener to allow us to mutate the application context before any
 * command-line tools are run.
 *
 * This class currently serves a single purpose, which is to create a fake "request" bean scope
 * to allow command-line tools to load RW beans that are request-scoped for use in the main webapp.
 *
 * See https://stackoverflow.com/a/28275111 which is the pattern we roughly followed here.
 */
public class ApplicationRunListener implements SpringApplicationRunListener {

  public ApplicationRunListener(SpringApplication application, String[] args) { }

  /**
   * Called immediately when the run method has first started. Can be used for very early
   * initialization.
   */
  @Override
  public void starting() {

  }

  /**
   * Called once the environment has been prepared, but before the {@link ApplicationContext} has
   * been created.
   *
   * @param environment the environment
   */
  @Override
  public void environmentPrepared(ConfigurableEnvironment environment) {

  }

  /**
   * Called once the {@link ApplicationContext} has been created and prepared, but before sources
   * have been loaded.
   *
   * @param context the application context
   */
  @Override
  public void contextPrepared(ConfigurableApplicationContext context) {
    context.getBeanFactory().registerScope("request", new SimpleThreadScope());
  }

  /**
   * Called once the application context has been loaded but before it has been refreshed.
   *
   * @param context the application context
   */
  @Override
  public void contextLoaded(ConfigurableApplicationContext context) {

  }

  /**
   * Called immediately before the run method finishes.
   *
   * @param context   the application context or null if a failure occurred before the context was
   *                  created
   * @param exception any run exception or null if run completed successfully.
   */
  @Override
  public void finished(ConfigurableApplicationContext context, Throwable exception) {

  }
}
