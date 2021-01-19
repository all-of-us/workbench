package org.pmiops.workbench.tools;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.springframework.beans.factory.annotation.Autowired;

public class DbAccessTierDeserializer implements JsonDeserializer<DbAccessTier> {

  @Autowired private AccessTierDao accessTierDao;

  @Override
  public DbAccessTier deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    return accessTierDao.findOneByShortName(json.getAsString());
  }
}
