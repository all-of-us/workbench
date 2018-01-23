package org.pmiops.workbench.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityType;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.model.Concept;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
public class ConceptCacheConfiguration {

    public static final String GENDER_RACE_ETHNICITY_ID = "gender_race_ethnicity";

    private static final Map<String, Class<?>> CONCEPT_MAP = new HashMap<>();

    static {
        CONCEPT_MAP.put(GENDER_RACE_ETHNICITY_ID, GenderRaceEthnicityConcept.class);
    }

    @Bean
    @Qualifier("conceptCacheConfiguration")
    LoadingCache<String, Object> getConceptCacheConfiguration(ConceptDao conceptDao) {
        // Cache configuration in memory for 24 hours.
        return CacheBuilder.<String, GenderRaceEthnicityConcept>newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build(new CacheLoader<String, Object>() {
                    @Override
                    public Object load(String key) {
                        Class<?> genderRaceEthnicityConcept = CONCEPT_MAP.get(key);
                        if (genderRaceEthnicityConcept == null) {
                            throw new IllegalArgumentException("Invalid config key: " + key);
                        }
                        List<Concept> conceptList = conceptDao.findGenderRaceEthnicityFromConcept();
                        Map<String, Map<Long, String>> returnMap = new HashMap<>();
                        for (GenderRaceEthnicityType type : GenderRaceEthnicityType.values()) {
                            Map<Long, String> filteredMap = conceptList.stream()
                                    .filter(c -> type.name().equalsIgnoreCase(c.getVocabularyId()))
                                    .collect(Collectors.toMap(Concept::getConceptId, Concept::getConceptName));
                            returnMap.put(type.name(), filteredMap);
                        }
                        return new GenderRaceEthnicityConcept(returnMap);
                    }
                });
    }

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    GenderRaceEthnicityConcept getGenderRaceEthnicityConcept(@Qualifier("conceptCacheConfiguration") LoadingCache<String, Object> conceptCacheConfiguration) throws ExecutionException {
        return (GenderRaceEthnicityConcept) conceptCacheConfiguration.get(GENDER_RACE_ETHNICITY_ID);
    }
}
