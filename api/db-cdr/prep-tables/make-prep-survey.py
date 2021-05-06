import argparse
import csv
from google.cloud import bigquery
from google.cloud import storage
from io import StringIO

# key = csv
surveys = {"ENGLISHBasics_DataDictionary": "Basics",
           "ENGLISHHealthCareAccessUtiliza_DataDictionary": "HealthCareAccessUtiliza",
           "ENGLISHLifestyle_DataDictionary": "Lifestyle",
           "ENGLISHOverallHealth_DataDictionary": "OverallHealth",
           "ENGLISHPersonalMedicalHistory_DataDictionary": "PersonalMedicalHistory"}


def main():
    # parse all provided args
    project, dataset, date = parse_args()

    # validate that all files for a specific date exist in the bucket
    files_exist(date)

    for name in surveys:
        rows = read_csv(name + '_' + date + '.csv')
        for row in rows:
            print(f"First Column: {row[0]}")

    big_query_client = bigquery.Client.from_service_account_json(
        "../../sa-key.json")
    query = """
    SELECT concept_code FROM `$project.dataset.concept`
    WHERE concept_name = "$concept_name" and vocabulary_id = "PPI" and
    concept_class_id = "Topic"
    """.replace("$project.dataset", project + "." + dataset).replace(
        "$concept_name", "Respiratory Conditions")
    query_job = big_query_client.query(query)
    rows = query_job.result()
    for row in rows:
        print(row.concept_code)


def parse_args():
    parser = argparse.ArgumentParser(prog='make-prep-survey',
                                     usage='%(prog)s --project <project> '
                                           '--dataset <dataset> '
                                           '--date <date>',
                                     description='Generate 3 csv files')
    parser.add_argument('--project', type=str, help='project name',
                        required=True)
    parser.add_argument('--dataset', type=str, help='dataset name',
                        required=True)
    parser.add_argument('--date', type=str, help='date for input file',
                        required=True)
    arg = parser.parse_args()
    return arg.project, arg.dataset, arg.date


def read_csv(file):
    blob = get_blob(file)
    blob = blob.download_as_string()
    blob = blob.decode('utf-8')
    blob = StringIO(blob)  # tranform bytes to string
    return csv.reader(blob)  # then use csv library to read the content


def files_exist(date):
    for name in surveys:
        file = name + '_' + date + '.csv'
        blob = get_blob(file)
        if not blob.exists():
            raise Exception(file +
                            " does not exist!")


def get_blob(file):
    storage_client = storage.Client.from_service_account_json(
        '../../sa-key.json')
    bucket = storage_client.get_bucket(
        'all-of-us-workbench-private-cloudsql')
    return bucket.blob('cb_prep_tables/redcap/' + file)


if __name__ == '__main__':
    main()
    print("done")
