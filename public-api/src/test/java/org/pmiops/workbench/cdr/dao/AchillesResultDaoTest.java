package org.pmiops.workbench.cdr.dao;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.AchillesAnalysis;
import org.pmiops.workbench.cdr.model.AchillesResult;
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
public class AchillesResultDaoTest {

    @Autowired
    private AchillesResultDao achillesResultDao;

    @Autowired
    private AchillesAnalysisDao achillesAnalysisDao;

    private AchillesAnalysis achillesAnalysis1;
    private AchillesAnalysis achillesAnalysis2;
    private AchillesAnalysis achillesAnalysis3;

    private AchillesResult achillesResult1;
    private AchillesResult achillesResult2;
    private AchillesResult achillesResult3;
    private AchillesResult achillesResult4;

    @Before
    public void setUp() {

        achillesAnalysis1=createAnalysis(3110L,"Survey Question Answer Count","survey_concept_id","question_concept_id","answer_concept_id","answer_value_string",null,"column","counts");
        achillesAnalysis2=createAnalysis(3111L,"Gender","survey_concept_id","question_concept_id","answer_concept_id","answer_value_string","gender_concept_id","column","counts");
        achillesAnalysis3=createAnalysis(3112L,"Age","survey_concept_id","question_concept_id","answer_concept_id","answer_value_string","age_decile","column","counts");

        achillesAnalysisDao.save(achillesAnalysis1);
        achillesAnalysisDao.save(achillesAnalysis2);
        achillesAnalysisDao.save(achillesAnalysis3);

        achillesResult1=createAchillesResult(2397L,3110L,"1586134","1000000","","Smoking",null,260L);
        achillesResult2=createAchillesResult(2380L,3111L,"1585855","2000000","","Drinking is the cause of failure",null,2345L);
        achillesResult3=createAchillesResult(2345L,3112L,"1586134","1000000","","Donot know",null,789L);
        achillesResult4=createAchillesResult(2346L,3112L,"1586134","2000000","","Prefer not to answer",null,890L);

        achillesResultDao.save(achillesResult1);
        achillesResultDao.save(achillesResult2);
        achillesResultDao.save(achillesResult3);
        achillesResultDao.save(achillesResult4);
    }

    @Test
    public void findAchillesResultByAnalysisId() throws Exception{
        final List<AchillesResult> list=achillesResultDao.findAchillesResultByAnalysisId(3112L);
        Assert.assertNotEquals(list,null);
    }



    private AchillesAnalysis createAnalysis(Long analysisId,String analysisName,String stratum1Name,String stratum2Name,String stratum3Name,String stratum4Name,String stratum5Name,String chartType,String dataType) {
        return new AchillesAnalysis()
            .analysisId(analysisId)
            .analysisName(analysisName)
            .stratum1Name(stratum1Name)
            .stratum2Name(stratum2Name)
            .stratum3Name(stratum3Name)
            .stratum4Name(stratum4Name)
            .stratum5Name(stratum5Name)
            .chartType(chartType)
            .dataType(dataType);
    }

    private AchillesResult createAchillesResult(Long id,Long analysisId,String stratum1,String stratum2,String stratum3,String stratum4,String stratum5,Long count){
        return new AchillesResult()
                .id(id)
                .analysisId(analysisId)
                .stratum1(stratum1)
                .stratum2(stratum2)
                .stratum3(stratum3)
                .stratum4(stratum4)
                .stratum5(stratum5)
                .countValue(count);
    }

    @After
    public void flush(){
        achillesAnalysisDao.delete(achillesAnalysis1);
        achillesAnalysisDao.delete(achillesAnalysis2);
        achillesAnalysisDao.delete(achillesAnalysis3);

        achillesResultDao.delete(achillesResult1);
        achillesResultDao.delete(achillesResult2);
        achillesResultDao.delete(achillesResult3);
        achillesResultDao.delete(achillesResult4);
    }

}
