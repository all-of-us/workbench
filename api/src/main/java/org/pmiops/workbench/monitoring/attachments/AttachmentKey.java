package org.pmiops.workbench.monitoring.attachments;

import com.google.common.collect.ImmutableMap;
import io.opencensus.metrics.data.AttachmentValue;
import io.opencensus.metrics.data.AttachmentValue.AttachmentValueString;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;

public enum AttachmentKey implements AttachmentKeyBase {

  NOTEBOOK_NAME("notebook_name"),
  NOTEBOOK_WORKSPACE_NAMESPACE("notebook_workspace_namespace");

  private String keyName;

  AttachmentKey(String keyName) {
    this.keyName = keyName;
  }

  @Override
  public String getKeyName() {
    return keyName;
  }

}
