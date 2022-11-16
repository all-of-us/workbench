package org.pmiops.workbench.notebooks;

import java.util.regex.Pattern;

/** Notebook files operation utils */
public class NotebookUtils {
  private NotebookUtils{}

  public static String NOTEBOOKS_WORKSPACE_DIRECTORY = "notebooks";
  public static String NOTEBOOK_EXTENSION = ".ipynb";

  public static final Pattern JUPYTER_NOTEBOOK_PATTERN =
      Pattern.compile(NOTEBOOKS_WORKSPACE_DIRECTORY + "/[^/]+(\\.(?i)(ipynb))$");
  public static final Pattern R_MARKDOWN_NOTEBOOK_PATTERN =
      Pattern.compile(NOTEBOOKS_WORKSPACE_DIRECTORY + "/[^/]+(\\.(?i)(rmd))$");

  public static boolean isJupyterNotebook(String name) {
    return JUPYTER_NOTEBOOK_PATTERN.matcher(name).matches();
  }

  public static boolean isRMarkDownNotebook(String name) {
    return R_MARKDOWN_NOTEBOOK_PATTERN.matcher(name).matches();
  }
}
