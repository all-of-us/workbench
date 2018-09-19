package org.pmiops.workbench.config;

import java.util.List;

public class SurveyModulesConfig {

  public String description;
  public String version;

  // A list of survey modules, in the order they should appear in the UI.
  public List<SurveyModuleConfig> surveys;

  public static class SurveyModuleConfig {
    public String name;
    public String description;
    public long conceptId;
    public long creationTimestamp;

    // A list of topics in the survey, in the order they should appear in the UI.
    public List<SurveyTopicConfig> topics;
  }

  public static class SurveyTopicConfig {
    public String name;
    public long conceptId;

    // An array of IDs of questions in the topic, in the order they should appear in the UI.
    public long[] questionConceptIds;
  }
}
