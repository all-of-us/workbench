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

  public static boolean isJupyterNotebookWithDirectory(String nameWithFileExtension) {
    return JUPYTER_NOTEBOOK_WITH_DIRECTORY_PATTERN.matcher(nameWithFileExtension).matches();
  }

  public static boolean isRMarkDownNotebookWithDirectory(String nameWithFileExtension) {
    return R_MARKDOWN_NOTEBOOK_WITH_DIRECTORY_PATTERN.matcher(nameWithFileExtension).matches();
  }

  public static boolean isJupyterNotebook(String nameWithFileExtension) {
    return nameWithFileExtension.endsWith(JUPYTER_NOTEBOOK_EXTENSION);
  }

  public static boolean isRMarkdownNotebook(String nameWithFileExtension) {
    return nameWithFileExtension.endsWith(R_MARKDOWN_NOTEBOOK_EXTENSION);
  }

  public static String withNotebookPath(String notebookName) {
    return NOTEBOOKS_WORKSPACE_DIRECTORY + "/" + notebookName;
  }
}
