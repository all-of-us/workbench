package org.pmiops.workbench.tools;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.mail.MailServiceImpl;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
  MailServiceImpl.class,
  MandrillApi.class,
})

/* example usage:
  ./project.rb send-email \
  --username joel@fake-research-aou.org \
  --contact thibault@broadinstitute.org \
  --disable \
  --lengths
*/
public class SendEmail extends Tool {

  private static final Option usernameOpt =
      Option.builder().longOpt("username").desc("User name").required().hasArg().build();

  private static final Option userContactOpt =
      Option.builder().longOpt("contact").desc("User contact email").required().hasArg().build();

  private static final Option disableOpt =
      Option.builder()
          .longOpt("disable")
          .desc(
              "If true, sends the DISABLE_USER egress email.  If false, sends the SUSPEND_COMPUTE egress email.")
          .required()
          .hasArg()
          .build();

  private static final Option fileLengthsOpt =
      Option.builder()
          .longOpt("lengths")
          .desc(
              "If true, sends the file lengths egress email.  If false, sends the standard egress email.")
          .required()
          .hasArg()
          .build();

  private static final Options options =
      new Options()
          .addOption(usernameOpt)
          .addOption(userContactOpt)
          .addOption(disableOpt)
          .addOption(fileLengthsOpt);

  @Bean
  public CommandLineRunner run(MailService mailService) {
    return args -> {
      CommandLine opts = new DefaultParser().parse(options, args);
      String username = opts.getOptionValue(usernameOpt.getLongOpt());
      String contactEmail = opts.getOptionValue(userContactOpt.getLongOpt());
      EgressRemediationAction action =
          Boolean.parseBoolean(opts.getOptionValue(disableOpt.getLongOpt()))
              ? EgressRemediationAction.DISABLE_USER
              : EgressRemediationAction.SUSPEND_COMPUTE;
      boolean fileLengths = Boolean.parseBoolean(opts.getOptionValue(fileLengthsOpt.getLongOpt()));

      DbUser user = new DbUser().setUsername(username).setContactEmail(contactEmail);
      if (fileLengths) {
        mailService.sendFileLengthsEgressRemediationEmail(user, action);
      } else {
        mailService.sendEgressRemediationEmail(user, action, "Jupyter");
      }
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(SendEmail.class, args);
  }
}
