package org.pmiops.workbench.api.util;

import org.pmiops.workbench.api.util.query.AbstractQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static AbstractQueryBuilder getQueryBuilder(String type) {
        AbstractQueryBuilder queryBuilder = queryBuilderCache.get(type );
        if(queryBuilder == null) throw new RuntimeException("Unknown queryBuilder type: " + type);
        return queryBuilder;
    }
}
