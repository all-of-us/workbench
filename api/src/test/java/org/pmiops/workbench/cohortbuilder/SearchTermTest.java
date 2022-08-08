package org.pmiops.workbench.cohortbuilder;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SearchTermTest {

  @BeforeEach
  void setUp() {}

  @AfterEach
  void tearDown() {}

  @Test
  void getTerm() {}

  @Test
  void getModifiedTerm() {}

  @Test
  void getEndsWithTerms() {}

  @Test
  void hasEndsWithOnly() {}

  @Test
  void hasModifiedTermOnly() {}

  @Test
  void hasEndsWithTermsAndModifiedTerm() {}

  @Test
  void hasNoTerms() {}

  @ParameterizedTest(name = "modifyTermMatchUseEndsWith: {0} {1}=>{2}")
  @MethodSource("getModifyTermMatchEndsWithParameters")
  void modifyTermMatchUseEndsWith(String testInput, String term, String expected) {
    SearchTerm actual = new SearchTerm(term, getStopWords());
    assertWithMessage(testInput).that(actual.getModifiedTerm()).isEqualTo(expected);
  }

  private static Stream<Arguments> getModifyTermMatchEndsWithParameters() {

    return Stream.of(
        Arguments.of("Search term: ", "lung", "+lung*"),
        Arguments.of("Search term: ", "+lung", "+lung*"),
        Arguments.of("Search term: ", "lung cancer", "+lung*+cancer*"),
        Arguments.of("Search term: ", "lung cancer", "+lung*+cancer*"),
        Arguments.of("Search term: ", "lung* cancer", "+lung*+cancer*"),
        Arguments.of("Search term: ", "lung cancer*", "+lung*+cancer*"),
        Arguments.of("Search term: ", "lung* cancer*", "+lung*+cancer*"),
        Arguments.of("Search term: ", "+lung cancer", "+lung*+cancer*"),
        Arguments.of("Search term: ", "lung +cancer", "+lung*+cancer*"),
        Arguments.of("Search term: ", "+lung +cancer", "+lung*+cancer*"),
        Arguments.of("Search term: ", "lung -cancer", "+lung*-cancer"),
        Arguments.of("Search term: ", "+lung -cancer", "+lung*-cancer"),
        Arguments.of("Search term: ", "lung* -cancer", "+lung*-cancer"),
        Arguments.of("Search term: ", "\"lung cancer\"", "+\"lung cancer\""),
        Arguments.of("Search term: ", "covid-19", "+\"covid-19\""),
        Arguments.of("Search term: ", "type-2-diabetes", "+\"type-2-diabetes\""),
        Arguments.of("Search term: ", "*statin pita", "+pita*"),
        Arguments.of("Search term: ", "*statin +pita", "+pita*"),
        Arguments.of("Search term: ", "-pita brea", "-pita+brea*"),
        Arguments.of("Search term: ", "*statin -pita", "-pita"),
        Arguments.of("Search term: ", "*statin *pita", ""),
        Arguments.of("Search term: ", "*statin other *pita", "+other*"),
        Arguments.of("Search term: ", "*statin other *pita -minus", "+other*-minus"),
        Arguments.of("Search term: ", "-\"my first phrase\"", "-\"my first phrase\""),
        Arguments.of("Search term: ", "+\"my second phrase\"", "+\"my second phrase\""),
        Arguments.of("Search term: ", "\"my second phrase\"", "+\"my second phrase\""),
        Arguments.of(
            "Search term: ", "-covid-19 -type-2-diabetes", "-\"covid-19\"-\"type-2-diabetes\""),
        Arguments.of(
            "Search term: ", "+covid-19 +type-2-diabetes", "+\"covid-19\"+\"type-2-diabetes\""),
        Arguments.of(
            "Search term: ", "covid-19 type-2-diabetes", "+\"covid-19\"+\"type-2-diabetes\""),
        Arguments.of("Search term: ", "-diabet", "-diabet"),
        Arguments.of("Search term: ", "+diabet", "+diabet*"),
        Arguments.of("Search term: ", "diabet", "+diabet*"),
        Arguments.of("Search term: ", "+++diabet", "+diabet*"),
        Arguments.of("Search term: ", "---diabet", "-diabet"),
        Arguments.of("Search term: ", "+diabet is from", "+diabet*"),
        Arguments.of("Search term: ", "+\"diabet is from\"", "+\"diabet is from\""),
        Arguments.of("Search term: ", "-\"diabet is from\"", "-\"diabet is from\""),
        Arguments.of("Search term: ", "how* is *it diabet ", "+diabet*")
        );
  }

  private static List<String> getStopWords() {
    // SELECT regexp_replace(group_concat(value),',','|') FROM
    // INFORMATION_SCHEMA.INNODB_FT_DEFAULT_STOPWORD;
    String stopwords =
        "a|about|an|are|as|at|be|by|com|de|en|for|from|how|i|in|is|it|la|of|on|or|that|the|this|to|was|what|when|where|who|will|with|und|the|www";
    return Arrays.asList(stopwords.split("\\|"));
  }
}
