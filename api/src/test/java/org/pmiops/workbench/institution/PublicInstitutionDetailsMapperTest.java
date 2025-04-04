package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement.MembershipRequirement;
import org.pmiops.workbench.model.InstitutionMembershipRequirement;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.PublicInstitutionDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@Import({PublicInstitutionDetailsMapperImpl.class, FakeClockConfiguration.class})
@DataJpaTest
public class PublicInstitutionDetailsMapperTest {
  @Autowired PublicInstitutionDetailsMapper mapper;

  @Test
  public void testDbToModelSuccess() {
    final DbInstitution dbInst =
        new DbInstitution()
            .setShortName("name")
            .setOrganizationTypeEnum(OrganizationType.INDUSTRY)
            .setDisplayName("displayName");
    final MembershipRequirement rtRequirement = MembershipRequirement.DOMAINS;

    assertThat(mapper.dbToModel(dbInst, rtRequirement))
        .isEqualTo(
            new PublicInstitutionDetails()
                .displayName(dbInst.getDisplayName())
                .shortName(dbInst.getShortName())
                .organizationTypeEnum(dbInst.getOrganizationTypeEnum())
                .registeredTierMembershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                .institutionalInitialCreditsExpirationBypassed(false));
  }
}
