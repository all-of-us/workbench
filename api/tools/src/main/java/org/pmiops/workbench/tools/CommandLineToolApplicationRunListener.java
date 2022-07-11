package org.pmiops.workbench.tools;

import java.util.logging.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.context.WebApplicationContext;

/**
 * A custom application listener to allow us to mutate the application context before any
 * command-line tools are run.
 *
 * <p>This class currently serves a single purpose, which is to create a fake "request" bean scope
 * to allow command-line tools to load RW beans that are request-scoped for use in the main webapp.
 *
 * <p>See https://stackoverflow.com/a/28275111 which is the pattern we roughly followed here. This
 * class is referenced from resources/META-INF/spring.factories, which causes this class to be
 * loaded before the Spring context is initialized.
 */
public class CommandLineToolApplicationRunListener implements SpringApplicationRunListener {

  private static final Logger log =
      Logger.getLogger(CommandLineToolApplicationRunListener.class.getName());

  public CommandLineToolApplicationRunListener(SpringApplication application, String[] args) {}

  @Override
  public void starting() {}

  @Override
  public void environmentPrepared(ConfigurableEnvironment environment) {}

  /**
   * Called once the {@link ApplicationContext} has been created and prepared, but before sources
   * have been loaded.
   *
   * @param context the application context
   */
  @Override
  public void contextPrepared(ConfigurableApplicationContext context) {
    log.info("Registering fake REQUEST scope to the context bean factory for command-line tools");
    context
        .getBeanFactory()
        .registerScope(WebApplicationContext.SCOPE_REQUEST, new SimpleThreadScope());
  }

  @Override
  public void contextLoaded(ConfigurableApplicationContext context) {}
}
