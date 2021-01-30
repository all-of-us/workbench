package org.pmiops.workbench.tools.cdrconfig;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;

/** Converts between an Access Tier ID and its DbAccessTier object when using GSON. */
public class DbAccessTierIdGsonAdapter extends TypeAdapter<DbAccessTier> {
  private final AccessTierDao accessTierDao;

  public DbAccessTierIdGsonAdapter(AccessTierDao accessTierDao) {
    this.accessTierDao = accessTierDao;
  }

  @Override
  public void write(JsonWriter out, DbAccessTier value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(value.getAccessTierId());
    }
  }

  @Override
  public DbAccessTier read(JsonReader in) throws IOException {
    if (in != null) {
      return accessTierDao.findOne(in.nextLong());
    } else {
      return null;
    }
  }
}
