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
import java.util.ArrayList;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class AchillesAnalysisDaoTest {

    @Autowired
    AchillesAnalysisDao dao;

    @Autowired
    AchillesResultDao achillesResultDao;

    private AchillesAnalysis achillesAnalysis1;
    private AchillesAnalysis achillesAnalysis2;
    private AchillesAnalysis achillesAnalysis3;
    private AchillesAnalysis achillesAnalysis4;
    private AchillesAnalysis achillesAnalysis5;

    private AchillesResult achillesResult1;
    private AchillesResult achillesResult2;
    private AchillesResult achillesResult3;
    private AchillesResult achillesResult4;
    private AchillesResult achillesResult5;
    private AchillesResult achillesResult6;

    @Before
    public void setUp() {

        achillesAnalysis1=createAnalysis(3110L,"Survey Question Answer Count","survey_concept_id","question_concept_id","answer_concept_id","answer_value_string",null,"column","counts");
        achillesAnalysis2=createAnalysis(3111L,"Gender","survey_concept_id","question_concept_id","answer_concept_id","answer_value_string","gender_concept_id","column","counts");
        achillesAnalysis3=createAnalysis(3112L,"Age","survey_concept_id","question_concept_id","answer_concept_id","answer_value_string","age_decile","column","counts");
        achillesAnalysis4=createAnalysis(3101L,"Gender","concept_id","ppi_sex_at_birth_concept_id",null,null,null,"pie","counts");
        achillesAnalysis5=createAnalysis(3102L,"Age","concept_id","age_decile",null,null,null,"column","counts");

        dao.save(achillesAnalysis1);
        dao.save(achillesAnalysis2);
        dao.save(achillesAnalysis3);
        dao.save(achillesAnalysis4);
        dao.save(achillesAnalysis5);

        achillesResult1=createAchillesResult(2397L,3110L,"1586134","1000000","","Smoking",null,260L,0L);
        achillesResult2=createAchillesResult(2380L,3111L,"1585855","2000000","","Drinking is the cause of failure",null,2345L,0L);
        achillesResult3=createAchillesResult(2345L,3112L,"1586134","1000000","","Donot know",null,789L,0L);
        achillesResult4=createAchillesResult(2346L,3112L,"1586134","2000000","","Prefer not to answer",null,890L,0L);
        achillesResult5=createAchillesResult(2456L,3101L,"104567","8507",null,null,null,20L,8L);
        achillesResult6=createAchillesResult(2457L,3102L,"104567","2",null,null,null,78L,90L);

        achillesResultDao.save(achillesResult1);
        achillesResultDao.save(achillesResult2);
        achillesResultDao.save(achillesResult3);
        achillesResultDao.save(achillesResult4);
        achillesResultDao.save(achillesResult5);
        achillesResultDao.save(achillesResult6);
    }

    @Test
    public void findAllAnalyses() throws Exception {
        /* Todo write more tests */
        final List<AchillesAnalysis> list = dao.findAll();
        Assert.assertNotEquals(list,null);
    }

    @Test
    public void findSurveyAnalysisResults() throws Exception{
        List<String> qids=Arrays.asList("1000000","2000000");
        final List<AchillesAnalysis> list=dao.findSurveyAnalysisResults("1586134",qids);
        Assert.assertNotEquals(list,null);
    }

    @Test
    public void findConceptAnalysisResults() throws Exception{
        List<Long> analysisIds = new ArrayList<>();
        analysisIds.add(3101L);
        analysisIds.add(3102L);
        List<AchillesAnalysis> aa = dao.findConceptAnalysisResults("104567",analysisIds);
        Assert.assertNotEquals(aa.get(0),null);
        Assert.assertNotEquals(aa.get(1),null);
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

    private AchillesResult createAchillesResult(Long id,Long analysisId,String stratum1,String stratum2,String stratum3,String stratum4,String stratum5,Long count, Long sourceCountValue){
        return new AchillesResult()
                .id(id)
                .analysisId(analysisId)
                .stratum1(stratum1)
                .stratum2(stratum2)
                .stratum3(stratum3)
                .stratum4(stratum4)
                .stratum5(stratum5)
                .countValue(count)
                .sourceCountValue(sourceCountValue);
    }

    @After
    public void flush(){
        dao.delete(achillesAnalysis1);
        dao.delete(achillesAnalysis2);
        dao.delete(achillesAnalysis3);
        dao.delete(achillesAnalysis4);
        dao.delete(achillesAnalysis5);

        achillesResultDao.delete(achillesResult1);
        achillesResultDao.delete(achillesResult2);
        achillesResultDao.delete(achillesResult3);
        achillesResultDao.delete(achillesResult4);
        achillesResultDao.delete(achillesResult5);
        achillesResultDao.delete(achillesResult6);
    }

}
