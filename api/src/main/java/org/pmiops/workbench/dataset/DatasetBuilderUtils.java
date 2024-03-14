package org.pmiops.workbench.dataset;

public class DatasetBuilderUtils {
  /** Split the string into an array of numbers (assuming comma separated) */
  public static String splitWithLineBreaks(String str, int chunkSize) {
    // Split the string into an array of numbers (assuming comma separated)
    String[] numbers = str.split(",");

    StringBuilder result = new StringBuilder();
    for (int i = 0; i < numbers.length; i += chunkSize) {
      // Add a comma before each number except the first in the chunk
      for (int j = Math.max(i, 1); j < Math.min(i + chunkSize, numbers.length); j++) {
        result.append(",").append(numbers[j]);
      }
      // Add the first number for each line
      result.append(numbers[i]);
      // Add line break after every chunk except the last one
      if (i + chunkSize < numbers.length) {
        result.append("\n");
      }
    }
    return result.toString();
  }
}
