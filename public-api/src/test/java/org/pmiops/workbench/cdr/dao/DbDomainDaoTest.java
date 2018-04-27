package org.pmiops.workbench.cdr.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class DbDomainDaoTest {

    @Autowired
    DbDomainDao dao;

    private DbDomain dbDomain1;
    private DbDomain dbDomain2;
    private DbDomain dbDomain3;
    private DbDomain dbDomain4;
    private DbDomain dbDomain5;

    @Before
    public void setUp() {

        dbDomain1 = createDbDomain("Domain1","Sample Domain");
        dbDomain2 = createDbDomain("Domain2","Sample Domain");
        dbDomain3 = createDbDomain("Domain3","Sample Domain");
        dbDomain4 = createDbDomain("Domain4","Sample Domain");
        dbDomain5 = createDbDomain("Domain5","Sample Domain");

        dao.save(dbDomain1);
        dao.save(dbDomain2);
        dao.save(dbDomain3);
        dao.save(dbDomain4);
        dao.save(dbDomain5);
    }

    @Test
    public void findAllDbDomains() throws Exception {
        /* Todo write more tests */
        final List<DbDomain> list = dao.findAll();
        assert(dbDomain1.getDomainId().equals("Domain1"));
        assert(list.get(0).getDomainId().equals(dbDomain1.getDomainId()));
    }

    @Test
    public void findDbDomainsByDbType() throws Exception {
        /* Todo write more tests */
        final List<DbDomain> list = dao.findByDbType("Sample Domain");
        assert(dbDomain1.getDomainId().equals("Domain1"));
        assert(list.get(0).getDomainId().equals(dbDomain1.getDomainId()));
    }

    @Test
    public void findDbDomainsByDbTypeAndConceptId() throws Exception {
        /* Todo write more tests */
        final List<DbDomain> list = dao.findByDbTypeAndAndConceptIdNotNull("Sample Domain");
        assert(dbDomain1.getDomainId().equals("Domain1"));
        assert(list.get(0).getDomainId().equals(dbDomain1.getDomainId()));
    }

    private DbDomain createDbDomain(String domainId, String dbType) {
        return new DbDomain()
                .domainId(domainId)
                .domainDisplay("Domain description for display")
                .domainDesc("Domain description")
                .dbType(dbType)
                .domainRoute("Domain Route")
                .conceptId(Long.valueOf(0))
                .countValue(Long.valueOf(0));
    }

}
