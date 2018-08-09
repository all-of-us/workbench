##COUNT GENERATION

This document describes what counts are generated for each of the data domains.

1. Total Person count is generated and stored with analysis id 1.
2. Person count by gender is generated and stored with analysis id 2.
3. Person count by year of birth is generated and stored with analysis id 3.
4. Person count by race is generated and stored with analysis id 4.
5. Person count by ethnicity is generated and stored with analysis id 5.
6. Person count by year of birth and gender is generated and stored with analysis id 10.
7. Person count by race and ethnicity is generated and stored with analysis id 12.


####1. EHR DATA

######Domains( VISIT, CONDITION, PROCEDURE, DRUG, OBSERVATION, MEASUREMENT):
1. Counts and source counts of distinct patients with at least one occurrence of the concepts in these domains are generated and stored in analysis id 3000.
   Most EHR data in OMOP has two concepts associated with it. The source concept and the standard concept that source maps to.
   Hence, we calculate two counts for each concept.
   Counts are the number of people that have the concept or some concept mapped to it. 
   Source counts are the number of people that have the concept as real EHR data. 
   In the case of source concepts that always map to a standard concept, the count value would be zero.
   In that case we set the countValue to be the same as sourceCountValue to make it more user-friendly in apis and front end development.
2. Counts and source counts of gender of patients are generated in the similar way and stored in analysis id 3101.
3. Counts and source counts of different age deciles of patient data are generated and stored in analysis id 3102.
4. Participant count in each of the domains is generated and stored with analysis id 3000.

####2. PPI SURVEYS

######Survey Modules (The Basics, Lifestyle, Overall health)

1. Response count for each of the survey question in each module is generated and stored with analysis id 3110.
2. Response count by gender is generated and stored with analysis id 3111.
3. Response count by age decile is generated and stored with analysis id 3112.
4. Participant count in each of the survey module is generated.

####3. MEASUREMENTS

1. Box distribution of value is generated for each measurement.
2. Box distribution of age with gender is generated for each measurement.
3. Counts and source counts of different gender, age for binned values are generated. (Values are binned by 10)
