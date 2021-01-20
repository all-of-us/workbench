package org.pmiops.workbench.tools;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;

public class DbAccessTierDeserializer implements JsonDeserializer<DbAccessTier> {

  private final AccessTierDao accessTierDao;

  public DbAccessTierDeserializer(AccessTierDao accessTierDao) {
    this.accessTierDao = accessTierDao;
  }

  @Override
  public DbAccessTier deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    return accessTierDao.findOneByShortName(json.getAsString());
  }
}
