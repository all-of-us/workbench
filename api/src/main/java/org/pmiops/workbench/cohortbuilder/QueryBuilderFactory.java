package org.pmiops.workbench.cohortbuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.pmiops.workbench.cohortbuilder.querybuilder.AbstractQueryBuilder;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory class that autowires different implementations of {@link AbstractQueryBuilder} and stores
 * them on a map. {@link org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey} should be used
 * to get an instance of {@link AbstractQueryBuilder}.
 */
@Component
// TODO:Remove when new search is finished - freemabd
public class QueryBuilderFactory {

  @Autowired private List<AbstractQueryBuilder> queryBuilders;

  private static final Map<FactoryKey, AbstractQueryBuilder> queryBuilderCache = new HashMap<>();

  @PostConstruct
  public void initQueryBuilderCache() {
    for (AbstractQueryBuilder queryBuilder : queryBuilders) {
      queryBuilderCache.put(queryBuilder.getType(), queryBuilder);
    }
  }

  /**
   * Users of this method should use the following: {@link
   * org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey}
   *
   * @param key
   * @return
   */
  public static AbstractQueryBuilder getQueryBuilder(FactoryKey key) {
    AbstractQueryBuilder queryBuilder = queryBuilderCache.get(key);
    if (queryBuilder == null) {
      throw new BadRequestException(String.format("Unknown queryBuilder type: %s", key.name()));
    }
    return queryBuilder;
  }
}
