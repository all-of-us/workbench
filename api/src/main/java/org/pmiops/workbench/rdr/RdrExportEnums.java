package org.pmiops.workbench.rdr;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic;

public class RdrExportEnums {

  private static final BiMap<SpecificPopulationEnum, RdrWorkspaceDemographic.RaceEthnicityEnum>
      CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_RACE_ETHNICITY =
          ImmutableBiMap
              .<SpecificPopulationEnum, RdrWorkspaceDemographic.RaceEthnicityEnum>builder()
              .put(SpecificPopulationEnum.RACE_AA, RdrWorkspaceDemographic.RaceEthnicityEnum.AA)
              .put(SpecificPopulationEnum.RACE_AIAN, RdrWorkspaceDemographic.RaceEthnicityEnum.AIAN)
              .put(
                  SpecificPopulationEnum.RACE_ASIAN,
                  RdrWorkspaceDemographic.RaceEthnicityEnum.ASIAN)
              .put(SpecificPopulationEnum.RACE_NHPI, RdrWorkspaceDemographic.RaceEthnicityEnum.NHPI)
              .put(SpecificPopulationEnum.RACE_MENA, RdrWorkspaceDemographic.RaceEthnicityEnum.MENA)
              .put(
                  SpecificPopulationEnum.RACE_HISPANIC,
                  RdrWorkspaceDemographic.RaceEthnicityEnum.HISPANIC)
              .put(
                  SpecificPopulationEnum.RACE_MORE_THAN_ONE,
                  RdrWorkspaceDemographic.RaceEthnicityEnum.MULTI)
              .build();

  private static final BiMap<SpecificPopulationEnum, RdrWorkspaceDemographic.AgeEnum>
      CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_AGE =
          ImmutableBiMap.<SpecificPopulationEnum, RdrWorkspaceDemographic.AgeEnum>builder()
              .put(SpecificPopulationEnum.AGE_CHILDREN, RdrWorkspaceDemographic.AgeEnum.AGE_0_11)
              .put(
                  SpecificPopulationEnum.AGE_ADOLESCENTS, RdrWorkspaceDemographic.AgeEnum.AGE_12_17)
              .put(SpecificPopulationEnum.AGE_OLDER, RdrWorkspaceDemographic.AgeEnum.AGE_65_74)
              .put(
                  SpecificPopulationEnum.AGE_OLDER_MORE_THAN_75,
                  RdrWorkspaceDemographic.AgeEnum.AGE_75_AND_MORE)
              .build();

  public static RdrWorkspaceDemographic.RaceEthnicityEnum specificPopulationToRaceEthnicity(
      SpecificPopulationEnum specificPopulationEnum) {
    if (CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_RACE_ETHNICITY.containsKey(specificPopulationEnum)) {
      return CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_RACE_ETHNICITY.get(specificPopulationEnum);
    } else {
      return null;
    }
  }

  public static RdrWorkspaceDemographic.AgeEnum specificPopulationToAge(
      SpecificPopulationEnum specificPopulationEnum) {
    if (CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_AGE.containsKey(specificPopulationEnum)) {
      return CLIENT_TO_RDR_WORKSPACE_DEMOGRAPHIC_AGE.get(specificPopulationEnum);
    } else {
      return null;
    }
  }
}
