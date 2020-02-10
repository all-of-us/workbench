package org.pmiops.workbench.zendesk;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;
import org.zendesk.client.v2.Zendesk;

@Configuration
public class ZendeskConfig {
  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public Zendesk getZendeskClient(WorkbenchConfig config, DbUser user) {
    // Note: this client attaches the current user's AoU username, but does not actually
    // authenticate as them. The client can be used for anonymous endpoints such as the request
    // create endpoint.
    // If we decided we need an authenticated client in the future, we could use a Zendesk "API
    // token" for this purpose, e.g. to get/update/close existing tickets.
    return new Zendesk.Builder(config.zendesk.host).setUsername(user.getUsername()).build();
  }
}
