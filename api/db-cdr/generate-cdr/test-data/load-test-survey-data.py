import argparse
import csv
import os
import shutil
from google.cloud import bigquery
from google.cloud import storage
from io import StringIO

# These are redcap files for Minute and SDOH Surveys
surveys = ['ENGLISHWinterMinuteSurveyOnCOV_DataDictionary',
           'ENGLISHSocialDeterminantsOfHea_DataDictionary']

# these are the column names we are interested
# in parsing out of the redcap files
headers = ['concept_code']

home_dir = "../csv"


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

    for name in surveys:
        rows = read_csv(get_filename(name, date))
        concept_codes = []

        for row in rows:
            concept_codes.append(row['Variable / Field Name'].lower())

        concept_codes_string = "'" + "','".join(concept_codes) + "'"

        # cleanup data if previously created
        print("deleting data for " + name + " in dataset " + destination)
        delete_from_observation(bigquery_client, destination,
                                concept_codes_string)
        print(
            "deletion complete data for " + name + " in dataset " + destination)

        rows = get_questions(bigquery_client, source, destination,
                             concept_codes_string)

        for row in rows:
            print(row.observation_id)


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


def delete_from_observation(bigquery_client, destination, concept_codes_string):
    query = (
        "DELETE FROM `all-of-us-ehr-dev.{destination}.observation` "
        "WHERE observation_source_concept_id in ("
        "SELECT concept_id FROM `all-of-us-ehr-dev.{destination}.concept` "
        "WHERE lower(concept_code) in ({codes}))"
            .format(destination=destination, codes=concept_codes_string)
    )
    query_job = bigquery_client.query(query)
    return query_job.result()


def get_questions(bigquery_client, source, destination, concept_codes_string):
    query = (
        "SELECT * FROM `all-of-us-ehr-dev.{source}.observation` "
        "WHERE observation_source_concept_id in ("
        "SELECT concept_id FROM `all-of-us-ehr-dev.{destination}.concept` "
        "WHERE lower(concept_code) in ({codes}))"
            .format(source=source, destination=destination,
                    codes=concept_codes_string)
    )
    query_job = bigquery_client.query(query)
    return query_job.result()


if __name__ == '__main__':
    main()
    print("done")
