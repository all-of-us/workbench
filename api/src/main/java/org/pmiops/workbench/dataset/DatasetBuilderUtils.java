package org.pmiops.workbench.dataset;

import java.util.Arrays;

public class DatasetBuilderUtils {
  private DatasetBuilderUtils() {}

  /** Split the string into an array of numbers (assuming comma separated) */
  public static String splitWithLineBreaks(String str, int chunkSize) {
    String[] numbers = str.split(",");
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < numbers.length; i++) {
      result.append(numbers[i]);

      // Add a comma after each number except the last one in the chunk
      if (i % chunkSize != chunkSize - 1 && i != numbers.length - 1) {
        result.append(",");
      }

      // Add line break after every chunk except the last one
      if (i % chunkSize == chunkSize - 1 && i != numbers.length - 1) {
        result.append(System.lineSeparator()).append(",");
      }
    }
    return result.toString();
  }

  public static String transformText(String input, int lineLengthLimit) {
    String[] lines = input.split("\n");
    StringBuilder result = new StringBuilder();

    for (String line : lines) {
      if (line.length() > lineLengthLimit) {
        result.append(splitWithLineBreaks(line, lineLengthLimit));
      } else {
        result.append(line);
      }
      result.append(System.lineSeparator());
    }
    return result.toString();
  }

}
