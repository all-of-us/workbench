package org.pmiops.workbench.tools;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.mail.MailSender;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.mail.MailServiceImpl;
import org.pmiops.workbench.mail.SendGridMailSender;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
  MailServiceImpl.class,
})

/* example usage:
  ./project.rb send-email \
  --email egress \
  --username joel@fake-research-aou.org \
  --contact your-email@example.com \
  --given_name Joel \
  --disable true

  ./project.rb send-email \
  --email initial_credits_dollar_threshold \
  --username joel@fake-research-aou.org \
  --contact your-email@example.com \
  --given_name Joel \
  --used_credits 150.00 \
  --remaining_balance 150.00

  ./project.rb send-email \
  --email initial_credits_exhaustion \
  --username joel@fake-research-aou.org \
  --contact your-email@example.com \
  --given_name Joel

  ./project.rb send-email \
  --email initial_credits_expiring \
  --username joel@fake-research-aou.org \
  --contact your-email@example.com \
  --given_name Joel \
  --expiration_date 2025-03-15

  ./project.rb send-email \
  --email initial_credits_expired \
  --username joel@fake-research-aou.org \
  --contact your-email@example.com \
  --given_name Joel \
  --expiration_date 2025-01-15
*/
public class SendEmail extends Tool {
  private static final Option whichEmailOpt =
      Option.builder()
          .longOpt("email")
          .desc(
              "Which email to send: egress, initial_credits_dollar_threshold, initial_credits_exhaustion, initial_credits_expiring, initial_credits_expired")
          .required()
          .hasArg()
          .build();

  private static final Option usernameOpt =
      Option.builder().longOpt("username").desc("User name").required().hasArg().build();

  private static final Option givenNameOpt =
      Option.builder().longOpt("given_name").desc("Given name").required().hasArg().build();

  private static final Option userContactOpt =
      Option.builder().longOpt("contact").desc("User contact email").required().hasArg().build();

  private static final Option disableOpt =
      Option.builder()
          .longOpt("disable")
          .desc("For egress email: if true, sends DISABLE_USER; if false, sends SUSPEND_COMPUTE")
          .hasArg()
          .build();

  private static final Option usedCreditsOpt =
      Option.builder()
          .longOpt("used_credits")
          .desc("For dollar_threshold email: amount of credits used (e.g., 150.00)")
          .hasArg()
          .build();

  private static final Option remainingBalanceOpt =
      Option.builder()
          .longOpt("remaining_balance")
          .desc("For dollar_threshold email: remaining balance (e.g., 150.00)")
          .hasArg()
          .build();

  private static final Option expirationDateOpt =
      Option.builder()
          .longOpt("expiration_date")
          .desc("For expiring/expired emails: expiration date in YYYY-MM-DD format")
          .hasArg()
          .build();

  private static final Options options =
      new Options()
          .addOption(whichEmailOpt)
          .addOption(usernameOpt)
          .addOption(userContactOpt)
          .addOption(givenNameOpt)
          .addOption(disableOpt)
          .addOption(usedCreditsOpt)
          .addOption(remainingBalanceOpt)
          .addOption(expirationDateOpt);

  @Bean
  MailSender mailSender(
      jakarta.inject.Provider<CloudStorageClient> cloudStorageClientProvider,
      jakarta.inject.Provider<WorkbenchConfig> workbenchConfigProvider) {
    return new SendGridMailSender(cloudStorageClientProvider, workbenchConfigProvider);
  }

  @Bean
  public CommandLineRunner run(MailService mailService) {
    return args -> {
      CommandLine opts = new DefaultParser().parse(options, args);
      String whichEmail = opts.getOptionValue(whichEmailOpt.getLongOpt());
      String username = opts.getOptionValue(usernameOpt.getLongOpt());
      String contactEmail = opts.getOptionValue(userContactOpt.getLongOpt());
      String givenName = opts.getOptionValue(givenNameOpt.getLongOpt());

      DbUser user =
          new DbUser().setUsername(username).setContactEmail(contactEmail).setGivenName(givenName);

      switch (whichEmail) {
        case "egress" -> {
          String disableValue = opts.getOptionValue(disableOpt.getLongOpt(), "false");
          EgressRemediationAction action =
              Boolean.parseBoolean(disableValue)
                  ? EgressRemediationAction.DISABLE_USER
                  : EgressRemediationAction.SUSPEND_COMPUTE;
          mailService.sendEgressRemediationEmail(user, action, "Jupyter");
          System.out.println("Sent egress email to " + contactEmail);
        }
        case "initial_credits_dollar_threshold" -> {
          double usedCredits =
              Double.parseDouble(opts.getOptionValue(usedCreditsOpt.getLongOpt(), "150.00"));
          double remainingBalance =
              Double.parseDouble(opts.getOptionValue(remainingBalanceOpt.getLongOpt(), "150.00"));
          double threshold = usedCredits / (usedCredits + remainingBalance);
          mailService.alertUserInitialCreditsDollarThreshold(
              user, threshold, usedCredits, remainingBalance);
          System.out.println("Sent initial_credits_dollar_threshold email to " + contactEmail);
        }
        case "initial_credits_exhaustion" -> {
          mailService.alertUserInitialCreditsExhausted(user);
          System.out.println("Sent initial_credits_exhaustion email to " + contactEmail);
        }
        case "initial_credits_expiring" -> {
          String expirationDateStr =
              opts.getOptionValue(expirationDateOpt.getLongOpt(), "2025-03-15");
          Timestamp expirationTimestamp = parseExpirationDate(expirationDateStr);
          DbUserInitialCreditsExpiration expiration =
              new DbUserInitialCreditsExpiration().setExpirationTime(expirationTimestamp);
          user.setUserInitialCreditsExpiration(expiration);
          mailService.alertUserInitialCreditsExpiring(user);
          System.out.println("Sent initial_credits_expiring email to " + contactEmail);
        }
        case "initial_credits_expired" -> {
          String expirationDateStr =
              opts.getOptionValue(expirationDateOpt.getLongOpt(), "2025-01-15");
          Timestamp expirationTimestamp = parseExpirationDate(expirationDateStr);
          DbUserInitialCreditsExpiration expiration =
              new DbUserInitialCreditsExpiration().setExpirationTime(expirationTimestamp);
          user.setUserInitialCreditsExpiration(expiration);
          mailService.alertUserInitialCreditsExpired(user);
          System.out.println("Sent initial_credits_expired email to " + contactEmail);
        }
        default -> System.err.println(
            "Unknown email type: "
                + whichEmail
                + ". Valid options: egress, initial_credits_dollar_threshold, initial_credits_exhaustion, initial_credits_expiring, initial_credits_expired");
      }
    };
  }

  private static Timestamp parseExpirationDate(String dateStr) {
    LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
    Instant instant = date.atStartOfDay(ZoneId.of("America/Chicago")).toInstant();
    return Timestamp.from(instant);
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(SendEmail.class, args);
  }
}
