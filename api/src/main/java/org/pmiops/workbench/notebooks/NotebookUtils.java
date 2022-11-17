package org.pmiops.workbench.notebooks;

import java.util.regex.Pattern;

/** Notebook files operation utils */
public class NotebookUtils {
  private NotebookUtils() {}

  public static String NOTEBOOKS_WORKSPACE_DIRECTORY = "notebooks";
  public static String JUPYTER_NOTEBOOK_EXTENSION = ".ipynb";
  public static String R_MARKDOWN_NOTEBOOK_EXTENSION = ".rmd";

  // Pattern matches directory and the file type, e.g. notebooks/file.ipynb
  public static final Pattern JUPYTER_NOTEBOOK_WITH_DIRECTORY_PATTERN =
      Pattern.compile(NOTEBOOKS_WORKSPACE_DIRECTORY + "/[^/]+(\\.(?i)(ipynb))$");
  // Pattern matches directory and the file type, e.g. notebooks/file.rmd
  public static final Pattern R_MARKDOWN_NOTEBOOK_WITH_DIRECTORY_PATTERN =
      Pattern.compile(NOTEBOOKS_WORKSPACE_DIRECTORY + "/[^/]+(\\.(?i)(rmd))$");

  public static boolean isJupyterNotebookWithDirectory(String name) {
    return JUPYTER_NOTEBOOK_WITH_DIRECTORY_PATTERN.matcher(name).matches();
  }

  public static boolean isRMarkDownNotebookWithDirectory(String name) {
    return R_MARKDOWN_NOTEBOOK_WITH_DIRECTORY_PATTERN.matcher(name).matches();
  }

  public static boolean isJupyterNotebook(String name) {
    return name.endsWith(JUPYTER_NOTEBOOK_EXTENSION);
  }

  public static boolean isRMarkdownNotebook(String name) {
    return name.endsWith(R_MARKDOWN_NOTEBOOK_EXTENSION);
  }
}
