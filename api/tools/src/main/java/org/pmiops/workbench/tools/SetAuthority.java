package org.pmiops.workbench.tools;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.Authority;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * See api/project.rb set-authority. Adds or removes authorities (permissions) from users in the db.
 */
@SpringBootApplication
@EnableJpaRepositories("org.pmiops.workbench.db.dao")
@EntityScan("org.pmiops.workbench.db.model")
public class SetAuthority {

  private static final Logger log = Logger.getLogger(SetAuthority.class.getName());

  @Bean
  public CommandLineRunner run(UserDao userDao) {
    return (args) -> {
      if (args.length != 3) {
        throw new IllegalArgumentException(
            "Expected email_list, authority_list_to_add, authority_list_to_rm. Got " + args);
      }
      Set<String> emails = new HashSet();
      emails.add("mark.fickett@staging.pmi-ops.org");
      Set<Authority> authoritiesToAdd = new HashSet();
      authoritiestoAdd.add(Authority.REVIEW_RESEARCH_PURPOSE);
      Set<Authority> authoritiesToRemove = new HashSet();
      boolean dryRun = true;
      int numErrors = 0;
      int numChanged = 0;

      Set<Authority> intersection = authoritiesToAdd.copy();
      intersection.retainAll(authoritiesToRemove);
      if (!intersection.isEmpty()) {
        throw new IllegalArgumentException(
            "Authorities lists overlap with " + intersection + ". Add: " + authoritiesToAdd
            + " Remove: " + authoritiesToRemove);
      }

      for (String email : emails) {
        User user = userDao.findUserByEmail(email);
        if (user == null) {
          log.log(Level.SEVERE, "No user for {1}.", email);
          numErrors++;
          continue;
        }
        Set<Authority> granted = user.getAuthorities();
        granted.addAll(authoritiesToAdd);
        granted.removeAll(authoritiesToRemove);
        if (!dryRun) {
          user.setAuthorities(granted);
        }
        numChanged++;
      }

      log.log(
          Level.INFO,
          "{1}. {2} users changed, {3} errors.",
          dryRun ? "Dry run done" : "Done",
          numChanged,
          numErrors);
    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(ConfigLoader.class).web(false).run(args);
  }
}
