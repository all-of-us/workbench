package org.pmiops.workbench.cdr.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class CustomCBCriteriaDaoImpl implements CustomCBCriteriaDao {

  public static class QueryAndParameters {

    private final String query;
    private final MapSqlParameterSource parameters;

    QueryAndParameters(String query, MapSqlParameterSource parameters) {
      this.query = query;
      this.parameters = parameters;
    }

    public String getQuery() {
      return query;
    }

    public MapSqlParameterSource getParameters() {
      return parameters;
    }
  }

  private static final String ENDS_WITH_WITHOUT_TERM =
      "select *\n"
          + "from %s.cb_criteria\n"
          + "where is_standard = :standard\n"
          + "and match(full_text) against(concat('+[', :domain, '_rank1]') in boolean mode)\n"
          + "and (%s)\n"
          + "order by est_count desc, name asc\n";

  private static final String ENDS_WITH_WITH_TERM =
      "select *\n"
          + "from %s.cb_criteria\n"
          + "where is_standard = :standard\n"
          + "and match(full_text) against(concat(:term, '+[', :domain, '_rank1]') in boolean mode)\n"
          + "and (%s)\n"
          + "order by est_count desc, name asc\n";

  private static final String DYNAMIC_SQL = "upper(name) like upper(%s)";

  private static final String LIMIT_OFFSET = "limit %s offset %s\n";

  private static final String OR = "\nor\n";

  @Autowired private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  @Autowired private CdrVersionDao cdrVersionDao;

  @Override
  public Page<DbCriteria> findCriteriaByDomainAndStandardAndNameEndsWith(
      String domain, Boolean standard, List<String> endsWithList, Pageable page) {
    QueryAndParameters queryAndParameters =
        generateQueryAndParameters(ENDS_WITH_WITHOUT_TERM, domain, standard, null, endsWithList);
    return new PageImpl<>(
        queryForPaginatedList(page, queryAndParameters),
        page,
        Objects.requireNonNull(count(queryAndParameters)));
  }

  @Override
  public Page<DbCriteria> findCriteriaByDomainAndStandardAndTermAndNameEndsWith(
      String domain, Boolean standard, String term, List<String> endsWithList, Pageable page) {
    QueryAndParameters queryAndParameters =
        generateQueryAndParameters(ENDS_WITH_WITH_TERM, domain, standard, term, endsWithList);
    return new PageImpl<>(
        queryForPaginatedList(page, queryAndParameters),
        page,
        Objects.requireNonNull(count(queryAndParameters)));
  }

  protected QueryAndParameters generateQueryAndParameters(
      String sql, String domain, Boolean standard, String term, List<String> endsWithList) {
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("domain", domain).addValue("standard", standard);
    if (term != null) {
      parameters.addValue("term", term);
    }
    StringJoiner joiner = new StringJoiner(OR);

    long cdrVersionId = CdrVersionContext.getCdrVersion().getCdrVersionId();
    Optional<DbCdrVersion> cdrVersionOptional = cdrVersionDao.findById(cdrVersionId);
    DbCdrVersion dbCdrVersion =
        cdrVersionOptional.orElseThrow(
            () ->
                new BadRequestException(
                    String.format("CDR version with ID %s not found", cdrVersionId)));

    IntStream.range(0, endsWithList.size())
        .forEach(
            idx -> {
              String endsWith = endsWithList.get(idx).replace("*", "%");
              String parameterName = "endsWith" + idx;
              parameters.addValue(parameterName, endsWith);
              joiner.add(String.format(DYNAMIC_SQL, ":" + parameterName));
            });

    return new QueryAndParameters(
        String.format(sql, dbCdrVersion.getCdrDbName(), joiner), parameters);
  }

  @NotNull
  private List<DbCriteria> queryForPaginatedList(
      Pageable page, QueryAndParameters queryAndParameters) {
    return namedParameterJdbcTemplate.query(
        queryAndParameters.getQuery()
            + String.format(LIMIT_OFFSET, page.getPageSize(), page.getOffset()),
        queryAndParameters.getParameters(),
        new DBCriteriaRowMapper());
  }

  @Nullable
  private Long count(QueryAndParameters queryAndParameters) {
    return namedParameterJdbcTemplate.queryForObject(
        queryAndParameters.getQuery().replace("*", "count(*)"),
        queryAndParameters.getParameters(),
        Long.class);
  }

  private static class DBCriteriaRowMapper implements RowMapper<DbCriteria> {
    @Override
    public DbCriteria mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
      return (new BeanPropertyRowMapper<>(DbCriteria.class)).mapRow(rs, rowNum);
    }
  }
}
