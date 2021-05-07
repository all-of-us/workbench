import argparse
import csv
import os
import shutil
from google.cloud import bigquery
from google.cloud import storage
from io import StringIO

all_seq = 1

home_dir = "../csv"

# key = csv prefix, value = csv output file name
surveys = {"ENGLISHBasics_DataDictionary": "Basics",
           "ENGLISHHealthCareAccessUtiliza_DataDictionary": "HealthCareAccessUtiliza",
           "ENGLISHLifestyle_DataDictionary": "Lifestyle",
           "ENGLISHOverallHealth_DataDictionary": "OverallHealth",
           "ENGLISHPersonalMedicalHistory_DataDictionary": "PersonalMedicalHistory"}
# these are the column names for the controlled and registered output files
headers = ['id', 'parent_id', 'code', 'name', 'type', 'min', 'max',
           'answers_bucketed']
# these are the headers for outputAll file
headersAll = ['id', 'parent_id', 'code', 'name', 'type', 'min', 'max',
              'registered_topic_suppressed', 'registered_question_suppressed',
              'registered_answer_bucketed', 'controlled_topic_suppressed',
              'controlled_question_suppressed', 'controlled_answer_bucketed']


def main():
    # parse all provided args
    project, dataset, date = parse_args()

    # validate that all files for a specific date exist in the bucket
    files_exist(date)

    # setup csv directory to hold output csv files
    if os.path.exists(home_dir):
        shutil.rmtree(home_dir)
    os.mkdir(home_dir)

    for name in surveys:
        rows = read_csv(name + '_' + date + '.csv')

        # open your file writers for this survey and write headers
        file_prefix = home_dir + "/" + surveys.get(name)
        csv_controlled = open(file_prefix + "_controlled.csv", 'w')
        csv_registered = open(file_prefix + "_registered.csv", 'w')
        csv_all = open(file_prefix + "_all.csv", 'w')
        controlled_writer = csv.DictWriter(csv_controlled, fieldnames=headers)
        controlled_writer.writeheader()
        registered_writer = csv.DictWriter(csv_registered, fieldnames=headers)
        registered_writer.writeheader()
        all_writer = csv.DictWriter(csv_all, fieldnames=headersAll)
        all_writer.writeheader()

        for row in rows:
            # print the first column of each row
            print(f"First Column: {row[0]}")

            # This is where your code will go

            write_row_all(all_writer, 0, "question_code", "question_label",
                          'SURVEY', None, None, None, None)

    # This will query the concept table using concept_name
    results = query_concept_table(project, dataset, "Respiratory Conditions")
    for result in results:
        print(result.concept_code)

    # when done remove directory and all files
    shutil.rmtree(home_dir)


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


def query_concept_table(project, dataset, concept_name):
    big_query_client = bigquery.Client.from_service_account_json(
        "../../sa-key.json")
    query = """
    SELECT concept_code FROM `$project.dataset.concept`
    WHERE concept_name = "$concept_name" and vocabulary_id = "PPI" and
    concept_class_id = "Topic"
    """.replace("$project.dataset", project + "." + dataset).replace(
        "$concept_name", concept_name)
    query_job = big_query_client.query(query)
    return query_job.result()


def write_row_all(writer, parent_id, code, name, item_type, min_val, max_val,
    item_flags, field_names):
    global all_seq
    new_row = {'id': all_seq, 'parent_id': parent_id, 'code': code,
               'name': name, 'type': item_type, 'min': min_val, 'max': max_val,
               'registered_topic_suppressed': '0',
               'registered_question_suppressed': '0',
               'registered_answer_bucketed': '0',
               'controlled_topic_suppressed': '0',
               'controlled_question_suppressed': '0',
               'controlled_answer_bucketed': '0'}
    if item_type == 'TOPIC':
        new_row['registered_topic_suppressed'] = item_flags[
            "REGISTERED_TOPIC_SUPPRESSED"]
        new_row['controlled_topic_suppressed'] = item_flags[
            "CONTROLLED_TOPIC_SUPPRESSED"]
    elif item_type == 'QUESTION':
        # we have to replace questionCode with short code in Field Annotation
        new_row['code'] = get_short_code(code, field_names)
        new_row['registered_question_suppressed'] = item_flags[
            "REGISTERED_QUESTION_SUPPRESSED"]
        new_row['registered_answer_bucketed'] = item_flags[
            "REGISTERED_ANSWERS_BUCKETED"]
        new_row['controlled_question_suppressed'] = item_flags[
            "CONTROLLED_QUESTION_SUPPRESSED"]
        new_row['controlled_answer_bucketed'] = item_flags[
            "CONTROLLED_ANSWERS_BUCKETED"]
    writer.writerow(new_row)
    all_seq += 1


def get_short_code(code, field_names):
    for item in field_names:
        if code in field_names:
            code = field_names[code]
        else:
            code
        return code


if __name__ == '__main__':
    main()
    print("done")
