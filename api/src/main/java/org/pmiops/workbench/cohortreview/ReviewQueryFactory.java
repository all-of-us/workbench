package org.pmiops.workbench.cohortreview;

import org.pmiops.workbench.cohortreview.querybuilder.ReviewQueryBuilder;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.PageFilterType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class that autowires different implementations of {@link ReviewQueryBuilder}
 * and stores them on a map. {@link PageFilterType}
 * should be used to get an instance of {@link ReviewQueryBuilder}.
 */
@Component
public class ReviewQueryFactory {

    @Autowired
    private List<ReviewQueryBuilder> queryBuilders;

    private static final Map<PageFilterType, ReviewQueryBuilder> queryBuilderCache = new HashMap<>();

    @PostConstruct
    public void initQueryBuilderCache() {
        for(ReviewQueryBuilder queryBuilder : queryBuilders) {
            queryBuilderCache.put(queryBuilder.getPageFilterType(), queryBuilder);
        }
    }

    /**
     * Users of this method should use the following:
     * {@link PageFilterType}
     *
     * @param key
     * @return
     */
    public static ReviewQueryBuilder getQueryBuilder(PageFilterType key) {
        ReviewQueryBuilder queryBuilder = queryBuilderCache.get(key);
        if (queryBuilder == null) {
            throw new BadRequestException(String.format(
                    "Unknown queryBuilder type: %s", key.name()));
        }
        return queryBuilder;
    }
}
