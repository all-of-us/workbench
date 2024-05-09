package org.pmiops.workbench.cohortbuilder;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SearchTerm {

  private static final int MIN_TERM_LENGTH_NO_SPECIAL_CHAR = 2;
  private final String term;
  private String modifiedTerm;
  private List<String> endsWithTerms;
  private final String stopWordPattern;

  public SearchTerm(String term, List<String> stopWords) {
    this.term = term;
    // filter: any parsed words that are stop words criteria
    this.stopWordPattern = "^(" + String.join(")$|^(", stopWords) + ")$";
    parseTermForSearch();
  }

  public String getTerm() {
    return term;
  }

  public String getCodeTerm() {
    // could just be a static method
    return term.replaceAll("[()+\"*]", "");
  }

  public String getModifiedTerm() {
    return modifiedTerm;
  }

  public List<String> getEndsWithTerms() {
    return endsWithTerms;
  }

  public boolean hasEndsWithOnly() {
    return !endsWithTerms.isEmpty() && modifiedTerm.isEmpty();
  }

  public boolean hasModifiedTermOnly() {
    return endsWithTerms.isEmpty() && !modifiedTerm.isEmpty();
  }

  public boolean hasEndsWithTermsAndModifiedTerm() {
    return !endsWithTerms.isEmpty() && !modifiedTerm.isEmpty();
  }

  public boolean hasNoTerms() {
    // do not use codeTerm
    return endsWithTerms.isEmpty() && modifiedTerm.isEmpty();
  }

  private void parseTermForSearch() {
    // add quoted pattern to the list for modifiedTerms
    final String quotedPattern = "([+-]?\\\"[^\\\"]*\\\")";
    List<String> quotedTerms = parseQuotedTerms(quotedPattern);
    // remove the quoted phrase/pattern
    List<String> words =
        new ArrayList<>(Arrays.asList(term.replaceAll(quotedPattern, "").split(" ")));
    // remove words that start with multiple special chars
    Pattern specialChars = Pattern.compile("[+\\*|\\-]{2,}|^\\*.*[+\\*\\-]$");
    words =
        words.stream()
            .filter(word -> !specialChars.matcher(word).find())
            .collect(Collectors.toList());
    // parse and set endsWithTerms
    parseAndSetEndsWithTerms(words);
    // parse and set modifiedTerm
    parseAndSetModifiedTerm(words, quotedTerms);
  }

  private List<String> parseQuotedTerms(String quotedPattern) {
    List<String> quotedTerms = new ArrayList<>();
    Pattern pattern = Pattern.compile(quotedPattern);
    Matcher matcher = pattern.matcher(term);
    while (matcher.find()) {
      quotedTerms.add(matcher.group().matches("^[+-].*") ? matcher.group() : "+" + matcher.group());
    }
    return quotedTerms;
  }

  private void parseAndSetModifiedTerm(List<String> filteredWords, List<String> quotedTerms) {
    // now process non-endsWith words
    List<String> words =
        filteredWords.stream().filter(word -> !word.startsWith("*")).collect(Collectors.toList());
    List<String> parsedTerms = new ArrayList<>(quotedTerms);
    words.stream()
        .forEach(
            word -> {
              if (word.matches("-\\w+(-\\w+)+")) {
                // -covid-19 -type-2-diabetes => -"covid-19" -"type-2-diabetes"
                parsedTerms.add("-\"" + word.substring(1) + "\"");
              } else if (word.matches("\\+\\w+(-\\w+)+")) {
                // +covid-19 +type-2-diabetes => +"covid-19" +"type-2-diabetes"
                parsedTerms.add("+\"" + word.substring(1) + "\"");
              } else if (word.matches("\\w+(-\\w+)+")) {
                // covid-19 type-2-diabetes => +"covid-19" +"type-2-diabetes"
                parsedTerms.add("+\"" + word + "\"");
              } else if (word.startsWith("-")) {
                // -diabet => -diabet
                parsedTerms.add(word);
              } else if (word.startsWith("+")) {
                // +diabet => +diabet*
                parsedTerms.add(word + "*");
              } else {
                // diabet => +diabet*
                parsedTerms.add("+" + word + "*");
              }
            });

    // fix multiple occurrence of +/-/*
    // filter: any parsed words that are stop words criteria
    // filter: any parsed words that fail MIN_TERM_LENGTH_NO_SPECIAL_CHAR criteria

    this.modifiedTerm =
        parsedTerms.stream()
            .map(x -> x.replaceAll("\\++", "\\+"))
            .map(x -> x.replaceAll("-+", "\\-"))
            .map(x -> x.replaceAll("\\*+", "\\*"))
            .filter(w -> !w.replaceAll("[+*\"-]", "").toLowerCase().matches(stopWordPattern))
            .filter(w -> w.replaceAll("[+*\"-]", "").length() >= MIN_TERM_LENGTH_NO_SPECIAL_CHAR)
            .collect(Collectors.joining(""));
  }

  @NotNull
  private void parseAndSetEndsWithTerms(List<String> filteredWords) {
    List<String> endsWith =
        filteredWords.stream()
            .filter(word -> word.startsWith("*"))
            .map(word -> word.replaceAll("\\*", "%"))
            .map(word -> word.replaceAll("([\\.\\?])", "\\\\$1"))
            .collect(Collectors.toList());
    // filter: any parsed words that fail MIN_TERM_LENGTH_NO_SPECIAL_CHAR criteria
    this.endsWithTerms =
        endsWith.stream()
            .filter(w -> !w.replaceAll("[+-]", "").toLowerCase().matches(stopWordPattern))
            .filter(w -> w.replaceAll("[+%-]", "").length() >= MIN_TERM_LENGTH_NO_SPECIAL_CHAR)
            .collect(Collectors.toList());
  }
}
