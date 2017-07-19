package org.pmiops.workbench.api;

import com.google.api.services.oauth2.model.Userinfoplus;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.pmiops.workbench.firecloud.Entity;
import org.pmiops.workbench.firecloud.Entity.EntityTypes;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortController implements CohortsApi {

  static final String COHORT_ID_PREFIX = "cohort_";

  static final String CRITERIA_ATTRIBUTE = "criteria";
  static final String COHORT_TYPE_ATTRIBUTE = "cohort_type";
  static final String DISPLAY_NAME_ATTRIBUTE = "display_name";
  static final String DESCRIPTION_ATTRIBUTE = "description";
  static final String CREATOR_ATTRIBUTE = "creator";
  static final String CREATION_TIME_ATTRIBUTE = "creationTime";
  static final String LAST_MODIFIED_TIME_ATTRIBUTE = "lastModifiedTime";

  private final FireCloudService fireCloudService;
  private final Provider<Userinfoplus> userInfoProvider;
  private final Clock clock;

  @Autowired
  CohortController(FireCloudService fireCloudService, Provider<Userinfoplus> userInfoProvider,
      Clock clock) {
    this.fireCloudService = fireCloudService;
    this.userInfoProvider = userInfoProvider;
    this.clock = clock;
  }

  @Override
  public ResponseEntity<CohortListResponse> getCohortsInWorkspace(String workspaceNamespace,
      String workspaceId) {
    List<Entity> entities = fireCloudService.getEntitiesInWorkspace(workspaceNamespace, workspaceId,
        EntityTypes.PARTICIPANT_SET);
    ImmutableList.Builder<Cohort> builder = ImmutableList.builder();
    for (Entity entity : entities) {
      if (entity.getName().startsWith(COHORT_ID_PREFIX)) {
        builder.add(fromEntity(entity));
      }
    }
    CohortListResponse response = new CohortListResponse();
    response.setItems(builder.build());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Cohort> createCohort(String workspaceNamespace, String workspaceId,
      Cohort cohort) {
    Entity entity = toEntity(cohort, true);
    Entity resultEntity = fireCloudService.createEntity(workspaceNamespace, workspaceId, entity);
    return ResponseEntity.ok(fromEntity(resultEntity));
  }

  @Override
  public ResponseEntity<Cohort> getCohort(String workspaceNamespace, String workspaceId,
      String cohortId) {
    Entity entity = fireCloudService.getEntity(workspaceNamespace, workspaceId,
        EntityTypes.PARTICIPANT_SET, toEntityName(cohortId));
    return ResponseEntity.ok(fromEntity(entity));
  }

  @Override
  public ResponseEntity<Cohort> updateCohort(String workspaceNamespace, String workspaceId,
      String cohortId, Cohort cohort) {
    Entity entity = toEntity(cohort, false);
    Entity updatedEntity = fireCloudService.updateEntity(workspaceNamespace, workspaceId, entity);
    return ResponseEntity.ok(fromEntity(updatedEntity));
  }

  @Override
  public ResponseEntity<Void> deleteCohort(String workspaceNamespace, String workspaceId,
      String cohortId) {
    fireCloudService.deleteEntity(workspaceNamespace, workspaceId, EntityTypes.PARTICIPANT_SET,
        cohortId);
    return ResponseEntity.ok(null);
  }

  private static String toEntityName(String cohortId) {
    return COHORT_ID_PREFIX + cohortId;
  }

  private static String fromEntityName(String entityId) {
    return entityId.substring(COHORT_ID_PREFIX.length());
  }

  private Entity toEntity(Cohort cohort, boolean create) {
    JsonObject attributes = new JsonObject();
    attributes.addProperty(CRITERIA_ATTRIBUTE, cohort.getCriteria());
    attributes.addProperty(COHORT_TYPE_ATTRIBUTE, cohort.getType());
    attributes.addProperty(DISPLAY_NAME_ATTRIBUTE, cohort.getName());
    attributes.addProperty(DESCRIPTION_ATTRIBUTE, cohort.getDescription());
    Instant instant = clock.instant();
    String time = new DateTime(instant, DateTimeZone.UTC).toString();
    if (create) {
      attributes.addProperty(CREATOR_ATTRIBUTE, userInfoProvider.get().getEmail());
      attributes.addProperty(CREATION_TIME_ATTRIBUTE, time);
    }
    attributes.addProperty(LAST_MODIFIED_TIME_ATTRIBUTE, time);
    return new Entity(toEntityName(cohort.getId()), EntityTypes.PARTICIPANT_SET, attributes);
  }

  private static Cohort fromEntity(Entity entity) {
    JsonObject attributes = entity.getAttributes();
    Cohort result = new Cohort();
    result.setId(fromEntityName(entity.getName()));
    result.setCriteria(attributes.get(CRITERIA_ATTRIBUTE).getAsString());
    result.setType(attributes.get(COHORT_TYPE_ATTRIBUTE).getAsString());
    result.setName(attributes.get(DISPLAY_NAME_ATTRIBUTE).getAsString());
    result.setDescription(attributes.get(DESCRIPTION_ATTRIBUTE).getAsString());
    result.setCreator(attributes.get(CREATOR_ATTRIBUTE).getAsString());
    result.setCreationTime(DateTime.parse(attributes.get(CREATION_TIME_ATTRIBUTE).getAsString()));
    result.setLastModifiedTime(DateTime.parse(attributes.get(LAST_MODIFIED_TIME_ATTRIBUTE)
        .getAsString()));
    return result;
  }
}
