package org.pmiops.workbench.dataset.builder;

import static com.google.cloud.bigquery.StandardSQLTypeName.ARRAY;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.KernelTypeEnum;

public final class NotebookCode {

  private String code;

  private NotebookCode(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public interface QueryConfiguration {
    QueryDomain setConfiguration(QueryJobConfiguration queryJobConfiguration);
  }

  public interface QueryDomain {
    Kernel setDomain(String domain);
  }

  public interface Kernel {
    DataSetName setKernel(KernelTypeEnum kernel);
  }

  public interface DataSetName {
    CdrName setDataSetName(String dataSetName);
  }

  public interface CdrName {
    Qualifier setCdrName(String cdrName);
  }

  public interface Qualifier {
    NotebookCodeCreator setQualifier(String qualifier);
  }

  public interface NotebookCodeCreator {
    String generateCode();
  }

  public static class Builder
      implements QueryConfiguration,
          QueryDomain,
          Kernel,
          DataSetName,
          CdrName,
          Qualifier,
          NotebookCodeCreator {

    private static final String CDR_STRING = "\\$\\{projectId}.\\$\\{dataSetId}.";
    private static final String PYTHON_CDR_ENV_VARIABLE =
        "\"\"\" + os.environ[\"WORKSPACE_CDR\"] + \"\"\".";
    // This is implicitly handled by bigrquery, so we don't need this variable.
    private static final String R_CDR_ENV_VARIABLE = "";
    private static final Map<KernelTypeEnum, String> KERNEL_TYPE_TO_ENV_VARIABLE_MAP =
        ImmutableMap.of(
            KernelTypeEnum.R, R_CDR_ENV_VARIABLE, KernelTypeEnum.PYTHON, PYTHON_CDR_ENV_VARIABLE);

    QueryJobConfiguration queryJobConfiguration;
    String domain;
    KernelTypeEnum kernel;
    String dataSetName;
    String cdrName;
    String qualifier;

    /** Private builder to prevent direct object creation */
    private Builder() {}

    /** Getting the instance method */
    public static QueryConfiguration getInstance() {
      return new Builder();
    }

    @Override
    public QueryDomain setConfiguration(QueryJobConfiguration queryJobConfiguration) {
      this.queryJobConfiguration = queryJobConfiguration;
      return this;
    }

    @Override
    public Kernel setDomain(String domain) {
      this.domain = domain;
      return this;
    }

    @Override
    public DataSetName setKernel(KernelTypeEnum kernel) {
      this.kernel = kernel;
      return this;
    }

    @Override
    public CdrName setDataSetName(String dataSetName) {
      this.dataSetName = dataSetName;
      return this;
    }

    @Override
    public Qualifier setCdrName(String cdrName) {
      this.cdrName = cdrName;
      return this;
    }

    @Override
    public NotebookCodeCreator setQualifier(String qualifier) {
      this.qualifier = qualifier;
      return this;
    }

    @Override
    public String generateCode() {
      // Define [namespace]_sql, query parameters (as either [namespace]_query_config
      // or [namespace]_query_parameters), and [namespace]_df variables
      String domainLowercase = domain.toLowerCase();
      String namespace = "dataset_" + qualifier + "_" + domainLowercase + "_";
      // Comments in R and Python have the same syntax
      String descriptiveComment =
          String.format(
              "# This query represents dataset \"%s\" for domain \"%s\" and was generated for %s",
              dataSetName, domainLowercase, cdrName);
      String query =
          queryJobConfiguration
              .getQuery()
              .replaceAll(CDR_STRING, KERNEL_TYPE_TO_ENV_VARIABLE_MAP.get(kernel));
      String formattedQuery = new BasicFormatterImpl().format(query);
      Map<String, QueryParameterValue> params = queryJobConfiguration.getNamedParameters();
      String prerequisites;
      String sqlSection;
      String dataFrameSection;
      String displayHeadSection;

      switch (kernel) {
        case PYTHON:
          prerequisites = "import pandas\n" + "import os";
          sqlSection =
              namespace + "sql = \"\"\"" + fillInQueryParams(formattedQuery, params) + "\"\"\"";
          dataFrameSection =
              namespace
                  + "df = pandas.read_gbq("
                  + namespace
                  + "sql, dialect=\"standard\", progress_bar_type=\"tqdm_notebook\")";
          displayHeadSection = namespace + "df.head(5)";
          break;
        case R:
          prerequisites = "library(bigrquery)";
          sqlSection =
              namespace
                  + "sql <- paste(\""
                  + fillInQueryParams(formattedQuery, params)
                  + "\", sep=\"\")";
          dataFrameSection =
              namespace
                  + "df <- bq_table_download(bq_dataset_query(Sys.getenv(\"WORKSPACE_CDR\"), "
                  + namespace
                  + "sql, billing=Sys.getenv(\"GOOGLE_PROJECT\")), bigint=\"integer64\")";
          displayHeadSection = "head(" + namespace + "df, 5)";
          break;
        default:
          throw new BadRequestException("Language " + kernel.toString() + " not supported.");
      }
      return new NotebookCode(
              prerequisites
                  + "\n\n"
                  + descriptiveComment
                  + "\n"
                  + sqlSection
                  + "\n\n"
                  + dataFrameSection
                  + "\n\n"
                  + displayHeadSection)
          .getCode();
    }

    private static String fillInQueryParams(
        String query, Map<String, QueryParameterValue> queryParameterValueMap) {
      return queryParameterValueMap.entrySet().stream()
          .map(param -> (Function<String, String>) s -> replaceParameter(s, param))
          .reduce(Function.identity(), Function::andThen)
          .apply(query)
          .replaceAll("unnest", "");
    }

    private static String replaceParameter(
        String s, Map.Entry<String, QueryParameterValue> parameter) {
      String value =
          ARRAY.equals(parameter.getValue().getType())
              ? nullableListToEmpty(parameter.getValue().getArrayValues()).stream()
                  .map(Builder::convertSqlTypeToString)
                  .collect(Collectors.joining(", "))
              : convertSqlTypeToString(parameter.getValue());
      String key = String.format("@%s", parameter.getKey());
      return s.replaceAll(key, value);
    }

    private static String convertSqlTypeToString(QueryParameterValue parameter) {
      switch (parameter.getType()) {
        case BOOL:
          return Boolean.parseBoolean(parameter.getValue()) ? "1" : "0";
        case INT64:
        case FLOAT64:
        case NUMERIC:
          return parameter.getValue();
        case STRING:
        case TIMESTAMP:
        case DATE:
          return String.format("'%s'", parameter.getValue());
        default:
          throw new RuntimeException();
      }
    }

    private static <T> List<T> nullableListToEmpty(List<T> nullableList) {
      return Optional.ofNullable(nullableList).orElse(new ArrayList<>());
    }
  }
}
