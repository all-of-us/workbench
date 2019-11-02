package org.pmiops.workbench.config

import com.google.appengine.api.utils.SystemProperty
import com.google.appengine.api.utils.SystemProperty.Environment.Value

class WorkbenchEnvironment @JvmOverloads constructor(// This is an appengine property specifying production build or development build.
        // This is only true when running locally.
        val isDevelopment: Boolean = SystemProperty.environment.value() == Value.Development, val applicationId: String = SystemProperty.applicationId.get())
