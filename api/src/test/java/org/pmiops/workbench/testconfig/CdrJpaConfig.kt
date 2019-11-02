package org.pmiops.workbench.testconfig

import javax.persistence.EntityManagerFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class CdrJpaConfig {

    // This allows injection of @PersistenceContext(unitName="cdr") in the context of tests;
    // it uses the same entity manager for both workbench and CDR dbs.
    @Bean(name = ["cdr"])
    fun cdrEntityManager(entityManagerFactory: EntityManagerFactory): EntityManagerFactory {
        return entityManagerFactory
    }
}
