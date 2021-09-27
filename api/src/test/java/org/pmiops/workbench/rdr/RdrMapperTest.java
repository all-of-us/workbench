package org.pmiops.workbench.rdr;

import static com.google.common.truth.Truth.assertThat;

import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.rdr.model.RdrWorkspace.AccessTierEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

public class RdrMapperTest extends SpringTest {

  @TestConfiguration
  @Import(RdrMapperImpl.class)
  static class Configuration {}

  private @Autowired RdrMapper rdrMapper;

  private static Stream<Arguments> accessTierCases() {
    return Stream.of(
        Arguments.of("controlled", AccessTierEnum.CONTROLLED),
        Arguments.of("registered", AccessTierEnum.REGISTERED),
        Arguments.of(null, AccessTierEnum.UNSET),
        Arguments.of("asdf", AccessTierEnum.UNSET));
  }

  @ParameterizedTest
  @MethodSource("accessTierCases")
  public void testMapRdrWorkspace_accessTiers(
      @Nullable String tierShortName, AccessTierEnum wantRdrTier) {
    DbAccessTier accessTier = new DbAccessTier();
    accessTier.setShortName(tierShortName);

    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setAccessTier(accessTier);

    DbWorkspace from = new DbWorkspace();
    from.setCdrVersion(cdrVersion);

    assertThat(rdrMapper.toRdrModel(from).getAccessTier()).isEqualTo(wantRdrTier);
  }
}
