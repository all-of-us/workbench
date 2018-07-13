COUNT GENERATION

This document describes what counts are generated for each of the data domains

Total Person count is generated and stored with analysis id 1.
Person count by gender is generated and stored with analysis id 2.
Person count by year of birth is generated and stored with analysis id 3.
Person count by race is generated and stored with analysis id 4.
Person count by ethnicity is generated and stored with analysis id 5.
Person count by year of birth and gender is generated and stored with analysis id id 10.
Person count by race and ethnicity is generated and stored with analysis id 12.


1. EHR DATA

   Domains( VISIT, CONDITION, PROCEDURE, DRUG, OBSERVATION, MEASUREMENT):
    a: Counts and source counts of people with one of the occurrence of the concepts in these domains are generated and stored in analysis id 3000.
    Counts are the number of people that have the concept occurred and source counts are the number of people that have the same concept as source concept. In case the count value is 0
    it is made to be same as the source count to avoid ambiguity.
    b. Counts and source counts of gender are generated in the similar way and stored in analysis id 3101.
    c. Counts and source counts of different age deciles are generated and stored in analysis id 3102.
    d: Participant count in each of the domains is generated and stored with analysis id 3000.

2. PPI SURVEYS

    Survey Modules (The Basics, Lifestyle, Overall health)

    a: Response count for each of the survey question in each module is generated and stored with analysis id 3110.
    b: Response count by gender is generated and stored with analysis id 3111.
    c: Response count by age decile is generated and stored with analysis id 3112.
    d: Participant count in each of the survey module is generated.

3. MEASUREMENTS

    a: Box distribution of value is generated for each measurement.
    b: Box distribution of age with gender is generated for each measurement.
    c: Counts and source counts of different gender, age for binned values are generated. (Values are binned by 10)
