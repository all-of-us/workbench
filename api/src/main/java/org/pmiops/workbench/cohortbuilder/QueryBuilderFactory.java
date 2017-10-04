package org.pmiops.workbench.cohortbuilder;

import org.pmiops.workbench.cohortbuilder.querybuilder.AbstractQueryBuilder;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class that autowires different implementations of {@link AbstractQueryBuilder}
 * and stores them on a map. {@link org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey}
 * should be used to get an instance of {@link AbstractQueryBuilder}.
 */
@Component
public class QueryBuilderFactory {

    @Autowired
    private List<AbstractQueryBuilder> queryBuilders;

    private static final Map<String, AbstractQueryBuilder> queryBuilderCache = new HashMap<>();

    @PostConstruct
    public void initQueryBuilderCache() {
        for(AbstractQueryBuilder queryBuilder : queryBuilders) {
            queryBuilderCache.put(queryBuilder.getType(), queryBuilder);
        }
    }

    /**
     * Users of this method should use the following:
     * {@link org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey#getKey(String)}
     *
     * @param type
     * @return
     */
    public static AbstractQueryBuilder getQueryBuilder(String type) {
        AbstractQueryBuilder queryBuilder = queryBuilderCache.get(type );
        if(queryBuilder == null) throw new BadRequestException("Unknown queryBuilder type: {0}".format(type));
        return queryBuilder;
    }
}
