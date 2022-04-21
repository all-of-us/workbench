import argparse
import csv
import os
import shutil
from google.cloud import bigquery
from google.cloud import storage
from io import StringIO

# Load test survey data into a destination dataset
# Example: python load-test-survey-data.py --date 2022-02-04
# --source survey_latest --destination test_R2019q4r3

# These are redcap files for Minute and SDOH Surveys
surveys = ['ENGLISHWinterMinuteSurveyOnCOV_DataDictionary',
           'ENGLISHSocialDeterminantsOfHea_DataDictionary']

# these are the column names we are interested
# in parsing out of the redcap files
headers = ['concept_code']

home_dir = "../../csv"


def main():
    # parse all provided args
    date, source, destination = parse_args()

    bigquery_client = init_bigquery_client()

    # validate that all files for a specific date exist in the bucket
    files_exist(date)

    # setup csv directory to hold output csv files
    if os.path.exists(home_dir):
        shutil.rmtree(home_dir)
    os.mkdir(home_dir)

    max_id = 100000000
    insert_id = max_id

    # cleanup data if previously created
    print("deleting survey data")
    delete_survey(bigquery_client, destination, max_id)
    print("survey data deletion complete")

    for name in surveys:
        rows = read_csv(get_filename(name, date))
        concept_codes = []

        for row in rows:
            concept_codes.append(row['Variable / Field Name'].lower())

        concept_codes_string = "'" + "','".join(concept_codes) + "'"

        # insert data for survey
        print("inserting data for " + name)
        insert_survey(bigquery_client, source, destination,
                      concept_codes_string, insert_id)
        print("data insertion complete")
        if name == "ENGLISHWinterMinuteSurveyOnCOV_DataDictionary":
            print("inserting survey version for " + name)
            insert_version(bigquery_client, source, destination,
                           concept_codes_string, insert_id)
            print("data insertion complete")

        rows = get_max_id(bigquery_client, destination)
        for row in rows:
            insert_id = row.id

    person_ids_old = get_person_ids_old(bigquery_client, destination, max_id)
    person_ids_new = get_person_ids_new(bigquery_client, destination,
                                        person_ids_old.total_rows)
    for old, new in zip(person_ids_old, person_ids_new):
        print("updating person_id from " + str(old.person_id) + " to " + str(
            new.person_id))
        update_person_id(bigquery_client, destination, old.person_id,
                         new.person_id)


# when done remove directory and all files
shutil.rmtree(home_dir)


def parse_args():
    parser = argparse.ArgumentParser(prog='load-test-data',
                                     usage='%(prog)s --date <date> '
                                           '--source <source-dataset> '
                                           '--destination <destination-dataset>',
                                     description='Generate test survey data')
    parser.add_argument('--date', type=str, help='date for input file',
                        required=True)
    parser.add_argument('--source', type=str, help='dataset name',
                        required=True)
    parser.add_argument('--destination', type=str, help='dataset name',
                        required=True)
    arg = parser.parse_args()
    return arg.date, arg.source, arg.destination


def read_csv(file):
    blob = get_blob(file)
    blob = blob.download_as_string()
    blob = blob.decode('utf-8')
    blob = StringIO(blob)  # tranform bytes to string
    return csv.DictReader(blob)  # then use csv library to read the content


def files_exist(date):
    for name in surveys:
        file = get_filename(name, date)
        blob = get_blob(file)
        if not blob.exists():
            raise Exception(file + " does not exist!")


def get_filename(name, date):
    file = name + '_' + date + '.csv'
    return file


def get_blob(file):
    storage_client = storage.Client.from_service_account_json(
        '../../../sa-key.json')
    bucket = storage_client.get_bucket(
        'all-of-us-workbench-private-cloudsql')
    return bucket.blob('redcap/' + file)


def init_bigquery_client():
    bigquery_client = bigquery.Client.from_service_account_json(
        '../../../sa-key.json')
    return bigquery_client


def get_max_id(bigquery_client, destination):
    query = (
        "SELECT MAX(observation_id) as id FROM "
        "`all-of-us-ehr-dev.{destination}.observation`"
            .format(destination=destination)
    )
    query_job = bigquery_client.query(query)
    return query_job.result()


def delete_survey(bigquery_client, destination, max_id):
    query = (
        "DELETE FROM `all-of-us-ehr-dev.{destination}.observation_ext` "
        "WHERE observation_id >= {max_id}"
            .format(destination=destination, max_id=max_id)
    )
    query_job = bigquery_client.query(query)
    query_job.result()
    query = (
        "DELETE FROM `all-of-us-ehr-dev.{destination}.observation` "
        "WHERE observation_id >= {max_id}"
            .format(destination=destination, max_id=max_id)
    )
    query_job = bigquery_client.query(query)
    return query_job.result()


def insert_survey(bigquery_client, source, destination, concept_codes_string,
    max_id):
    query = (
        "INSERT INTO `all-of-us-ehr-dev.{destination}.observation` "
        "(observation_id, person_id, observation_concept_id, observation_date, "
        "observation_datetime, observation_type_concept_id, value_as_number, "
        "value_as_string, value_as_concept_id, qualifier_concept_id, "
        "unit_concept_id, provider_id, visit_occurrence_id, visit_detail_id, "
        "observation_source_value, observation_source_concept_id, "
        "unit_source_value, qualifier_source_value, value_source_concept_id, "
        "value_source_value, questionnaire_response_id"
        ")"
        "SELECT ROW_NUMBER() OVER (ORDER BY observation_id) "
        "+ {max_id}, person_id, "
        "observation_concept_id, observation_date, observation_datetime, "
        "observation_type_concept_id, value_as_number, value_as_string, "
        "value_as_concept_id, qualifier_concept_id, unit_concept_id, "
        "provider_id, visit_occurrence_id, visit_detail_id, "
        "observation_source_value, observation_source_concept_id, "
        "unit_source_value, qualifier_source_value, value_source_concept_id, "
        "value_source_value, questionnaire_response_id "
        "FROM `all-of-us-ehr-dev.{source}.observation` "
        "WHERE observation_source_concept_id in ("
        "SELECT concept_id FROM `all-of-us-ehr-dev.{destination}.concept` "
        "WHERE lower(concept_code) in ({codes}))"
            .format(source=source, destination=destination,
                    codes=concept_codes_string, max_id=max_id)
    )
    query_job = bigquery_client.query(query)
    return query_job.result()


def insert_version(bigquery_client, source, destination,
    concept_codes_string, max_id):
    query = ("INSERT INTO `all-of-us-ehr-dev.{destination}.observation_ext`"
             "SELECT ROW_NUMBER() OVER (ORDER BY observation_id) "
             "+ {max_id}, src_id, survey_version_concept_id "
             "FROM `all-of-us-ehr-dev.{source}.observation_ext` "
             "WHERE observation_id in ("
             "SELECT observation_id "
             "FROM `all-of-us-ehr-dev.{source}.observation` "
             "WHERE observation_source_concept_id in ("
             "SELECT concept_id FROM `all-of-us-ehr-dev.test_R2019q4r3.concept` "
             "WHERE lower(concept_code) in ({codes})))"
             .format(source=source, destination=destination,
                     codes=concept_codes_string, max_id=max_id)
             )
    query_job = bigquery_client.query(query)
    return query_job.result()


def get_person_ids_old(bigquery_client, destination, max_id):
    query = ("SELECT DISTINCT person_id FROM "
             "`all-of-us-ehr-dev.{destination}.observation` "
             "WHERE observation_id >= {max_id} ORDER BY person_id"
             .format(destination=destination, max_id=max_id)
             )
    query_job = bigquery_client.query(query)
    return query_job.result()


def get_person_ids_new(bigquery_client, destination, limit):
    query = ("SELECT person_id FROM "
             "`all-of-us-ehr-dev.{destination}.person` "
             "ORDER BY person_id LIMIT {limit}"
             .format(destination=destination, limit=limit)
             )
    query_job = bigquery_client.query(query)
    return query_job.result()


def update_person_id(bigquery_client, destination, old, new):
    query = ("UPDATE `all-of-us-ehr-dev.{destination}.observation` "
             "SET person_id = {new} "
             "WHERE person_id = {old}"
             .format(destination=destination, old=old, new=new)
             )
    query_job = bigquery_client.query(query)
    return query_job.result()


if __name__ == '__main__':
    main()
    print("done")
