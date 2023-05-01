package org.pmiops.workbench.tools;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Authority;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * See api/project.rb set-authority. Adds or removes authorities (permissions) from users in the db.
 */
@Configuration
public class SetAuthority extends Tool {

  private static final Logger log = Logger.getLogger(SetAuthority.class.getName());
  private static final String ALL_AUTHORITIES = "ALL";

  private Set<String> commaDelimitedStringToSet(String str) {
    return new HashSet<>(Arrays.asList(str.split(",")));
  }

  private Set<Authority> commaDelimitedStringToAuthoritySet(String str) {
    Set<Authority> auths = new HashSet();
    for (String value : commaDelimitedStringToSet(str)) {
      String cleanedValue = value.trim().toUpperCase();
      if (cleanedValue.isEmpty()) {
        continue;
      }
      if (ALL_AUTHORITIES.equals(cleanedValue)) {
        return new HashSet<>(Arrays.asList(Authority.values()));
      }
      auths.add(Authority.valueOf(cleanedValue));
    }
    return auths;
  }

  @Bean
  public CommandLineRunner run(UserDao userDao) {
    return (args) -> {
      // User-friendly command-line parsing is done in devstart.rb, so we do only simple positional
      // argument parsing here.
      if (args.length != 5) {
        throw new IllegalArgumentException(
            "Expected 5 args (email_list, authorities, remove, dry_run, confirm_add_all). Got "
                + Arrays.asList(args));
      }
      Set<String> emails = commaDelimitedStringToSet(args[0]);
      String authoritiesArgument = args[1];
      Set<Authority> authorities = commaDelimitedStringToAuthoritySet(authoritiesArgument);
      boolean remove = Boolean.valueOf(args[2]);
      boolean dryRun = Boolean.valueOf(args[3]);
      boolean confirmAddAll = Boolean.valueOf(args[4]);
      int numUsers = 0;
      int numErrors = 0;
      int numChanged = 0;

      if (!remove
          && authoritiesArgument.toUpperCase().contains(ALL_AUTHORITIES)
          && !confirmAddAll) {
        String addAllWarning =
            "Adding ALL authorities is redundant and rarely useful; to transitively grant all authorities, simply add the all-encompassing DEVELOPER authority. If you still want to add ALL authorities, set the --confirm_add_all flag.";
        log.log(Level.SEVERE, addAllWarning);
        throw new IllegalArgumentException(addAllWarning);
      }

      for (String email : emails) {
        numUsers++;
        DbUser user = userDao.findUserByUsername(email);
        if (user == null) {
          log.log(Level.SEVERE, "No user for {0}.", email);
          numErrors++;
          continue;
        }
        // JOIN authorities, not usually fetched.
        user = userDao.findUserWithAuthorities(user.getUserId());

        Set<Authority> granted = user.getAuthoritiesEnum();
        Set<Authority> updated = new HashSet(granted);
        if (remove) {
          updated.removeAll(authorities);
        } else {
          updated.addAll(authorities);
        }
        if (!updated.equals(granted)) {
          if (!dryRun) {
            user.setAuthoritiesEnum(updated);
            userDao.save(user);
          }
          numChanged++;
          log.log(Level.INFO, "{0} {1} => {2}.", new Object[] {email, granted, updated});
        } else {
          log.log(Level.INFO, "{0} unchanged.", email);
        }
      }

      log.log(
          Level.INFO,
          "{0}. {1} of {2} users changed, {3} errors.",
          new Object[] {dryRun ? "Dry run done" : "Done", numChanged, numUsers, numErrors});
    };
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(SetAuthority.class, args);
  }
}
