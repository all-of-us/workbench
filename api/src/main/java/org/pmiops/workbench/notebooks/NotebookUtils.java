package org.pmiops.workbench.notebooks;

import com.google.common.annotations.VisibleForTesting;
import java.util.regex.Pattern;
import org.pmiops.workbench.exceptions.NotImplementedException;

/** Notebook files operation utils */
public class NotebookUtils {
  private NotebookUtils() {}

  public static String NOTEBOOKS_WORKSPACE_DIRECTORY = "notebooks";
  public static String JUPYTER_NOTEBOOK_EXTENSION = ".ipynb";
  public static String R_MARKDOWN_NOTEBOOK_EXTENSION = ".Rmd";
  public static String R_SCRIPT_EXTENSION = ".R";
  public static String SAS_EXTENSION = ".sas";

  // Pattern matches directory and the file type, e.g. notebooks/file.ipynb
  public static final Pattern JUPYTER_NOTEBOOK_WITH_DIRECTORY_PATTERN =
      Pattern.compile(NOTEBOOKS_WORKSPACE_DIRECTORY + "/[^/]+(\\.(?i)(ipynb))$");
  // Pattern matches directory and the file type, e.g. notebooks/file.Rmd
  public static final Pattern R_MARKDOWN_NOTEBOOK_WITH_DIRECTORY_PATTERN =
      Pattern.compile(NOTEBOOKS_WORKSPACE_DIRECTORY + "/[^/]+(\\.(?i)(Rmd))$");

  public static final Pattern R_SCRIPT_WITH_DIRECTORY_PATTERN =
      Pattern.compile(NOTEBOOKS_WORKSPACE_DIRECTORY + "/[^/]+(\\.(?i)(R))$");

  public static final Pattern SAS_WITH_DIRECTORY_PATTERN =
      Pattern.compile(NOTEBOOKS_WORKSPACE_DIRECTORY + "/[^/]+(\\.(?i)(sas))$");

  public static boolean isJupyterNotebookWithDirectory(String nameWithFileExtension) {
    return JUPYTER_NOTEBOOK_WITH_DIRECTORY_PATTERN.matcher(nameWithFileExtension).matches();
  }

  public static boolean isRMarkDownNotebookWithDirectory(String nameWithFileExtension) {
    return R_MARKDOWN_NOTEBOOK_WITH_DIRECTORY_PATTERN.matcher(nameWithFileExtension).matches();
  }

  public static boolean isRScriptWithDirectory(String nameWithFileExtension) {
    return R_SCRIPT_WITH_DIRECTORY_PATTERN.matcher(nameWithFileExtension).matches();
  }

  public static boolean isRStudioFileWithDirectory(String nameWithFileExtension) {
    return isRMarkDownNotebookWithDirectory(nameWithFileExtension)
        || isRScriptWithDirectory(nameWithFileExtension);
  }

  public static boolean isSasWithDirectory(String nameWithFileExtension) {
    return SAS_WITH_DIRECTORY_PATTERN.matcher(nameWithFileExtension).matches();
  }

  public static boolean isJupyterNotebook(String nameWithFileExtension) {
    return nameWithFileExtension.endsWith(JUPYTER_NOTEBOOK_EXTENSION);
  }

  public static boolean isRmdNotebook(String nameWithFileExtension) {
    return nameWithFileExtension.endsWith(R_MARKDOWN_NOTEBOOK_EXTENSION);
  }

  public static boolean isRScriptFile(String nameWithFileExtension) {
    return nameWithFileExtension.endsWith(R_SCRIPT_EXTENSION);
  }

  public static boolean isRStudioFile(String nameWithFileExtension) {
    return isRmdNotebook(nameWithFileExtension) || isRScriptFile(nameWithFileExtension);
  }

  private static boolean isSasFile(String nameWithFileExtension) {
    return nameWithFileExtension.endsWith(SAS_EXTENSION);
  }

  /** Appends file type extension e.g. ipynb if the file does not have one. */
  public static String appendFileExtensionIfMissing(
      String fromNotebookNameWithExtension, String newName) {
    if (isRmdNotebook(fromNotebookNameWithExtension)) {
      return NotebookUtils.withRMarkdownExtension(newName);
    } else if (isJupyterNotebook(fromNotebookNameWithExtension)) {
      return NotebookUtils.withJupyterNotebookExtension(newName);
    } else if (isSasFile(fromNotebookNameWithExtension)) {
      return NotebookUtils.withSasExtension(newName);
    } else if (isRScriptFile(fromNotebookNameWithExtension)) {
      return NotebookUtils.withRFileExtension(newName);
    } else {
      throw new NotImplementedException(
          String.format(
              "%s is a type of file that is not yet supported", fromNotebookNameWithExtension));
    }
  }

  @VisibleForTesting
  public static String withJupyterNotebookExtension(String notebookName) {
    return notebookName.endsWith(JUPYTER_NOTEBOOK_EXTENSION)
        ? notebookName
        : notebookName.concat(JUPYTER_NOTEBOOK_EXTENSION);
  }

  private static String withRMarkdownExtension(String notebookName) {
    return notebookName.endsWith(R_MARKDOWN_NOTEBOOK_EXTENSION)
        ? notebookName
        : notebookName.concat(R_MARKDOWN_NOTEBOOK_EXTENSION);
  }

  private static String withRFileExtension(String notebookName) {
    return notebookName.endsWith(R_SCRIPT_EXTENSION)
        ? notebookName
        : notebookName.concat(R_SCRIPT_EXTENSION);
  }

  private static String withSasExtension(String notebookName) {
    return notebookName.endsWith(SAS_EXTENSION) ? notebookName : notebookName.concat(SAS_EXTENSION);
  }

  public static String withNotebookPath(String notebookName) {
    return NOTEBOOKS_WORKSPACE_DIRECTORY + "/" + notebookName;
  }
}
