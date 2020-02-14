package org.pmiops.workbench.cohortbuilder.util;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.pmiops.workbench.exceptions.BadRequestException;

public final class MySqlSearchUtil {

  public static String modifySearchTerm(String term) {
    if (term == null || term.trim().isEmpty()) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Please provide a valid search term: \"%s\" is not valid.", term));
    }
    String[] keywords = term.split("\\W+");
    if (keywords.length == 1 && keywords[0].length() <= 3) {
      return "+\"" + keywords[0];
    }

    return IntStream.range(0, keywords.length)
        .filter(i -> keywords[i].length() > 2)
        .mapToObj(
            i -> {
              if ((i + 1) != keywords.length) {
                return "+\"" + keywords[i] + "\"";
              } else if (keywords[i].contains("-")) {
                return keywords[i];
              }
              return "+" + keywords[i] + "*";
            })
        .collect(Collectors.joining());
  }
}
