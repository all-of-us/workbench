package org.pmiops.workbench.cohortbuilder.util;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.cache.MySQLStopWords;

public class ScratchTest {
  @Mock
  private Provider<MySQLStopWords> mySQLStopWordsProvider;


  @BeforeEach
  public void setup(){
    MySQLStopWords mySQLStopWords = new MySQLStopWords(getStopWords());
    when(mySQLStopWordsProvider.get()).thenReturn(mySQLStopWords);
  }

  @ParameterizedTest(name = "modifyTermMatch: {0}->{1}")
  @MethodSource("getModifyTermMatchEndsWithParameters")
  void modifyTermMatch(String testInput, String term, String expected) {
    // modifyTermMatch() not called for numeric arguments like "001" or "001.1".
    assertWithMessage(testInput)
        .that(this.modifyTermMatchUseEndsWith(term))
        .isEqualTo(expected);
  }

  protected Map<String,String> modifyTermMatchUseEndsWith(String term){
    Map<String,String> searchTerms = new HashMap<>();
    term = removeStopWords(term);

    return searchTerms;
  }


  private static Stream<Arguments> getModifyTermMatchEndsWithParameters() {

    return Stream.of(
        // special chars are filtered by the UI-except ("\"", "+", "-", "*")
        /**
         * lung, lung*, +lung → +lung*
         *
         * <p>lung cancer, lung* cancer, lung cancer*, lung* cancer*, +lung cancer, lung +cancer,
         * +lung +cancer → +lung*+cancer*
         *
         * <p>lung -cancer, +lung -cancer, lung* -cancer → +lung*-cancer
         *
         * <p>“lung cancer” → +”lung cancer”
         *
         * <p>covid-19, +covid-19 → +”covid-19”
         *
         * <p>*statin pita, *statin +pita, *statin pita* → +pita*
         */

        // starts with special chars
        Arguments.of("Starts with special char '\"'", "\"lung can\"", "\"lung can\"")

        );

  }

  private String removeStopWords(String term) {
    List<String> stopWords = mySQLStopWordsProvider.get().getStopWords();
    term =
        Arrays.stream(term.split("\\s+"))
            .filter(w -> !stopWords.contains(w))
            .collect(Collectors.joining(" "));
    return term;
  }
  private static List<String> getStopWords() {
    // SELECT * FROM INFORMATION_SCHEMA.INNODB_FT_DEFAULT_STOPWORD;
    String stopwords =
        "a\n" + "about\n" + "an\n" + "are\n" + "as\n" + "at\n" + "be\n" + "by\n" + "com\n" + "de\n"
            + "en\n" + "for\n" + "from\n" + "how\n" + "i\n" + "in\n" + "is\n" + "it\n" + "la\n"
            + "of\n" + "on\n" + "or\n" + "that\n" + "the\n" + "this\n" + "to\n" + "was\n" + "what\n"
            + "when\n" + "where\n" + "who\n" + "will\n" + "with\n" + "und\n" + "the\n" + "www\n";
    return Arrays.asList(stopwords.split("\n"));
  }

}
