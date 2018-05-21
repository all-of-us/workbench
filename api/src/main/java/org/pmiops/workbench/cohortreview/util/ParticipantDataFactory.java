package org.pmiops.workbench.cohortreview.util;

import org.pmiops.workbench.model.Condition;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.Drug;
import org.pmiops.workbench.model.ParticipantData;

import java.util.function.Supplier;

public class ParticipantDataFactory {

  public enum ParticipantType {
    DRUG(Drug::new),
    CONDITION(Condition::new);

    private Supplier<ParticipantData> constructor;

    private ParticipantType(Supplier<ParticipantData> constructor) {
      this.constructor = constructor;
    }
  }

  public static ParticipantData createParticipantData(DomainType domainType) {
    for (ParticipantType pt : ParticipantType.values()) {
      if (domainType.name().equals(pt.name())) {
        return pt.constructor.get();
      }
    }
    return null;
  }
}
