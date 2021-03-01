package org.pmiops.workbench.ras;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class RasLinkServiceTest {
RasLinkServiceImpl rasLinkService = new RasLinkServiceImpl();
OauthHelper oauthHelper = new OauthHelper("https://stsstg.nih.gov/auth/oauth/v2/token");

  @Test
  public void rasTest() throws Exception {
    // rasLinkService.getLink("12345");

    //String token = oauthHelper.swapAuthenticationCode1("4ed11c18-bb53-4e4b-9fa8-cdd3894edd8a");

    GoogleOauthHelper oauthHelper = new GoogleOauthHelper();
    oauthHelper.rasLinkFlow("1e5195e5-9a7e-477e-96db-2326f65d6192");
    System.out.println("~~~~~");
  }
}
