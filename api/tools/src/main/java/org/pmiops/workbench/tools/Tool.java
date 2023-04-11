package org.pmiops.workbench.tools;

import org.pmiops.workbench.db.Params;
import org.pmiops.workbench.db.WorkbenchDbConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

// This is a common ancestor that brings in the main database configuration.
@Configuration
@Import({Params.class, WorkbenchDbConfig.class})
public abstract class Tool {}
