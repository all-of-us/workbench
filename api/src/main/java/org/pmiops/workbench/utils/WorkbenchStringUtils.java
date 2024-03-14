package org.pmiops.workbench.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generic utilities for string manipulation. The name "StringUtils" was already taken by Apache
 * Commons.
 */
public class WorkbenchStringUtils {
  /**
   * Transform an input string of many lines into a result string also with many lines, converting
   * any line which is too long to multiple lines in a best-effort process. See {@link
   * org.pmiops.workbench.utils.WorkbenchStringUtilsTest} for examples.
   *
   * @param input string
   * @param limit max line length
   * @param tokenSeparator a string (e.g. ",") representing where it is acceptable to split lines
   * @param lineSeparator the string used to split lines in the input and output strings
   * @return the transformed string
   */
  public static String splitTooLongLines(
      String input, int limit, String tokenSeparator, String lineSeparator) {
    return Arrays.stream(input.split(lineSeparator))
        .flatMap(
            line ->
                line.length() < limit
                    ? Stream.of(line)
                    : splitByLength(line, limit, tokenSeparator).stream())
        .collect(Collectors.joining(lineSeparator));
  }

  /**
   * Split a line into multiple lines, taking a best-effort approach to ensure that each resulting
   * line length is below the limit. See {@link org.pmiops.workbench.utils.WorkbenchStringUtilsTest}
   * for examples.
   *
   * @param line the input string, assumed to be a single line
   * @param limit max line length
   * @param tokenSeparator a string (e.g. ",") representing where it is acceptable to split lines
   * @return the resulting list of split lines
   */
  public static List<String> splitByLength(String line, int limit, String tokenSeparator) {
    String[] tokens = line.split(tokenSeparator);
    ArrayList<String> outLines = new ArrayList<>();
    StringBuilder currentLine = new StringBuilder();

    for (int i = 0; i < tokens.length; i++) {
      String nextToken = tokens[i];
      if (currentLine.isEmpty()) {
        // if we have not yet started building the current line, simply add the token
        currentLine.append(nextToken);
      } else {
        // add the next token plus the separator, if they fit
        if (currentLine.length() + tokenSeparator.length() + nextToken.length() <= limit) {
          currentLine.append(tokenSeparator).append(nextToken);
        } else {
          // complete the current line
          // Note: this may go over the limit by up to `tokenSeparator.length()`
          currentLine.append(tokenSeparator);

          // add current line to output and initialize the next line
          outLines.add(currentLine.toString());
          currentLine = new StringBuilder(nextToken);
        }
      }
    }

    if (!currentLine.isEmpty()) {
      outLines.add(currentLine.toString());
    }

    return outLines;
  }
}
