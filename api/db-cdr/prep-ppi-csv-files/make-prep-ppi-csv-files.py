import argparse
import csv
import os
import shutil
from io import StringIO
from os import listdir
from os.path import isfile, join

from google.cloud import bigquery
from google.cloud import storage

# we will increment this as the row number as we write each row in each output file
id_controlled = 1
id_registered = 1
id_all = 1

home_dir = "../csv"

# key = csv prefix, value = csv output file name
surveys = {"ENGLISHBasics_DataDictionary": "prep_ppi_basics",
           "ENGLISHHealthCareAccessUtiliza_DataDictionary": "prep_ppi_health_care_access",
           "ENGLISHLifestyle_DataDictionary": "prep_ppi_lifestyle",
           "ENGLISHOverallHealth_DataDictionary": "prep_ppi_overall_health",
           "ENGLISHPersonalMedicalHistory_DataDictionary": "prep_ppi_personal_medical_history"}

# field annotation flags
annotation_flags = {"REGISTERED_TOPIC_SUPPRESSED": "0",
                    "REGISTERED_QUESTION_SUPPRESSED": "0",
                    "REGISTERED_ANSWERS_BUCKETED": "0",
                    "REGISTERED_ANSWER_SUPPRESSED": "0",
                    "CONTROLLED_TOPIC_SUPPRESSED": "0",
                    "CONTROLLED_QUESTION_SUPPRESSED": "0",
                    "CONTROLLED_ANSWERS_BUCKETED": "0",
                    "CONTROLLED_ANSWER_SUPPRESSED": "0"}

# these are the column names for the controlled and registered output files
headers = ['id', 'parent_id', 'code', 'name', 'type', 'min', 'max',
           'answers_bucketed']

# these are the headers for outputAll file
headersAll = ['id', 'parent_id', 'code', 'name', 'type', 'min', 'max',
              'registered_topic_suppressed', 'registered_question_suppressed',
              'registered_answer_bucketed', 'registered_answer_suppressed',
              'controlled_topic_suppressed',
              'controlled_question_suppressed', 'controlled_answer_bucketed',
              'controlled_answer_suppressed']


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
        print("Reading & Parsing... " + name)

        # open your file writers for this survey and write headers
        controlled_writer, registered_writer, all_writer, controlled_file, registered_file, all_file \
            = open_writers_with_headers(name)

        # made these global in order to add rows in selected output files
        global id_controlled
        global id_registered
        global id_all

        # will use when the row_number = 1 to add in as the first row
        row_number = 0
        id_controlled = 1
        id_registered = 1
        id_all = 1

        # initialize survey_ids
        survey_id_all = 0
        survey_id_controlled = 0
        survey_id_registered = 0

        # initializing these variables in the case that the topic is null
        topic_id_all = 0
        topic_id_controlled = 0
        topic_id_register = 0

        for row in rows:

            if process_row(row):
                field_annotation, field_annotation_array, question_label, question_code, \
                text_validation, choices, min_value, \
                max_value = parse_row_columns(row)
                row_number += 1

                item_flags = {}
                for item in annotation_flags:
                    if item in field_annotation:
                        item_flags[item] = 1
                    else:
                        item_flags[item] = 0

                # for the first row of survey
                if row_number == 1:
                    survey_id_all = id_all
                    write_row_all(all_writer, 0, question_code, question_label,
                                  'SURVEY', None, None, item_flags, None)

                    survey_id_controlled = id_controlled
                    write_row(controlled_writer, 0, question_code,
                              question_label,
                              'SURVEY', None, None, item_flags, 'controlled',
                              None)

                    survey_id_registered = id_registered
                    write_row(registered_writer, 0, question_code,
                              question_label,
                              'SURVEY', None, None, item_flags, 'registered',
                              None)
                    continue

                # contains the long code as key and short code as key
                # EX: CompletelyQuitAgePreferNotToAnswer=AttemptQuitSmoking_CompletelyQuitAgePreferNo
                # items that are marked as suppressed will be stored with the key as flag
                # and the values as a list
                long_code_to_short_code = {}
                answer_suppression = {}
                for item in field_annotation_array:
                    if item not in annotation_flags:
                        split_codes = item.split('=')
                        # we only want the long_code = short_code so we filter out the answer_suppression flags
                        # EX:"CONTROLLED_ANSWER_SUPPRESSED"=WhatRaceEthnicity_AIAN looks similar to long_code=short_code

                        if "CONTROLLED_ANSWER_SUPPRESSED" not in split_codes and \
                            "REGISTERED_ANSWER_SUPPRESSED" not in split_codes:
                            # in the case that it doesn't have a code ex: Launched 5/30/2017 (PTSC) & 12/10/2019 (CE).
                            if len(split_codes) > 1:
                                long_code_to_short_code[
                                    split_codes[0].strip().lower()] = \
                                    split_codes[1].strip().lower()

                        if "CONTROLLED_ANSWER_SUPPRESSED" in split_codes:
                            answer_suppression.setdefault(
                                "CONTROLLED_ANSWER_SUPPRESSED", [])
                            answer_suppression[
                                "CONTROLLED_ANSWER_SUPPRESSED"].append(
                                split_codes[1].lower())

                        if "REGISTERED_ANSWER_SUPPRESSED" in split_codes:
                            answer_suppression.setdefault(
                                "REGISTERED_ANSWER_SUPPRESSED", [])
                            answer_suppression[
                                "REGISTERED_ANSWER_SUPPRESSED"].append(
                                split_codes[1].lower())

                ############ Topic ############
                if len(row['Section Header']) > 0:
                    header = row['Section Header']

                    # find the new lines and index to get the header up to the new line
                    # to do: need to figure out what to do with first topic in per_med_his
                    position = header.find("\n")
                    if position == -1:
                        header = row['Section Header'].replace(
                            "Thanks for your answers.", "")
                    else:
                        header = row['Section Header'][:position].replace(
                            "Thanks for your answers.", "")

                    # get the concept codes for the topics and write topics into file
                    topic_codes = query_topic_code(project, dataset,
                                                   header.replace("'", "\\'"))

                    topic_id_all = id_all
                    topic_code = ''
                    for code in topic_codes:
                        topic_code = code['concept_code']
                    write_row_all(all_writer, survey_id_all, topic_code, header,
                                  'TOPIC', None, None, item_flags,
                                  long_code_to_short_code)

                    # in the case that we have topic_suppressed we want to skip that row
                    if item_flags["CONTROLLED_TOPIC_SUPPRESSED"] != 1:
                        topic_id_controlled = id_controlled
                        write_row(controlled_writer, survey_id_controlled,
                                  topic_code, header
                                  , 'TOPIC', None, None, item_flags,
                                  'controlled', long_code_to_short_code)

                    if item_flags["REGISTERED_TOPIC_SUPPRESSED"] != 1:
                        topic_id_register = id_registered
                        write_row(registered_writer, survey_id_registered,
                                  topic_code, header
                                  , 'TOPIC', None, None, item_flags,
                                  'registered', long_code_to_short_code)

                ############ Question and Answer ############
                # if there is a topic use it otherwise use the survey as parent_id
                parent_id_all = survey_id_all if topic_id_all == 0 else topic_id_all
                parent_id_controlled = survey_id_controlled if topic_id_controlled == 0 else topic_id_controlled
                parent_id_registered = survey_id_registered if topic_id_register == 0 else topic_id_register

                min_value = ''
                max_value = ''
                is_numeric_answer = False

                if (
                    text_validation == 'integer' or text_validation == 'number') and \
                    row['Text Validation Min'] and row[
                    'Text Validation Max'] != '':
                    min_value = row['Text Validation Min']
                    max_value = row['Text Validation Max']
                    is_numeric_answer = True

                # store the id of the question to use as parent_id for answers
                question_id_all = id_all
                # write out question
                write_row_all(all_writer, parent_id_all, question_code,
                              question_label, 'QUESTION', min_value, max_value,
                              item_flags, long_code_to_short_code)

                # answers for outputAll file
                add_all_answers(all_writer, question_code, choices,
                                question_id_all, None,
                                None, item_flags, long_code_to_short_code)

                # in the case that the question is not suppressed
                if item_flags["CONTROLLED_QUESTION_SUPPRESSED"] != 1:
                    # write out question
                    question_id_controlled = id_controlled
                    write_row(controlled_writer, parent_id_controlled,
                              question_code, question_label
                              , 'QUESTION', None, None, item_flags,
                              'controlled', long_code_to_short_code)

                    # for questions with numeric response, we create an answer row named 'Select a value'
                    if is_numeric_answer:
                        add_numeric_answer(controlled_writer,
                                           question_id_controlled,
                                           min_value, max_value, item_flags,
                                           'controlled')
                    # write out answer
                    else:
                        add_answers(controlled_writer, choices, question_code,
                                    question_id_controlled
                                    , None, None, item_flags, 'controlled',
                                    long_code_to_short_code, answer_suppression)

                if item_flags["REGISTERED_QUESTION_SUPPRESSED"] != 1:
                    question_id_registered = id_registered
                    write_row(registered_writer, parent_id_registered,
                              question_code, question_label
                              , 'QUESTION', None, None, item_flags,
                              'registered', long_code_to_short_code)

                    if is_numeric_answer:
                        add_numeric_answer(registered_writer,
                                           question_id_registered,
                                           min_value, max_value, item_flags,
                                           'registered')
                    else:
                        add_answers(registered_writer, choices, question_code,
                                    question_id_registered
                                    , None, None, item_flags, 'registered',
                                    long_code_to_short_code, answer_suppression)

    flush_and_close_files(controlled_file, registered_file, all_file)

    # copy local output files and upload it to the bucket
    storage_client = storage.Client.from_service_account_json(
        '../../sa-key.json')
    bucket = storage_client.get_bucket('all-of-us-workbench-private-cloudsql')
    files = [f for f in listdir(home_dir) if isfile(join(home_dir, f))]
    for f in files:
        blob = bucket.blob('redcap/' + date + '/' + f)
        blob.upload_from_filename(home_dir + "/" + f)

    # when done remove directory and all files
    shutil.rmtree(home_dir)

    # copy static_surveys(Cope and Family History) to redcap date folder
    # Family History survey is static right now until curation fixes it in 2022
    # We need to potentially automate the cope survey if possible
    blobs = bucket.list_blobs(
        prefix='redcap/static_surveys/',
        delimiter='/')

    for blob in blobs:
        if blob.name.split('/')[2] != '':
            new_name = 'redcap/' + date + '/' + \
                       blob.name.split('/')[2]
            bucket.copy_blob(blob, bucket, new_name=new_name)


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
    return csv.DictReader(blob)  # then use csv library to read the content


def files_exist(date):
    for name in surveys:
        file = name + '_' + date + '.csv'
        blob = get_blob(file)
        if not blob.exists():
            raise Exception(file +
                            " does not exist!")


def process_row(row):
    return row['Variable / Field Name'].lower() != 'record_id' \
           and row['Field Type'].lower() != 'descriptive' \
           and 'please specify' not in row['Field Label'].lower()


def parse_row_columns(row):
    field_annotation = row['Field Annotation']
    field_annotation_array = []
    if field_annotation != '  ':
        for item in field_annotation.split(','):
            field_annotation_array.append(item.strip())
    # replace all '\n' with empty string
    question_label = row['Field Label'].replace('\n', '')
    question_code = row['Variable / Field Name']
    text_validation = row['Text Validation Type OR Show Slider Number']
    choices = row['Choices, Calculations, OR Slider Labels']
    min_value = row['Text Validation Min']
    max_value = row['Text Validation Max']
    return field_annotation, field_annotation_array, question_label, question_code, \
           text_validation, choices, min_value, max_value


def get_blob(file):
    storage_client = storage.Client.from_service_account_json(
        '../../sa-key.json')
    bucket = storage_client.get_bucket(
        'all-of-us-workbench-private-cloudsql')
    return bucket.blob('redcap/' + file)


def query_topic_code(project, dataset, concept_name):
    big_query_client = bigquery.Client.from_service_account_json(
        "../../sa-key.json")
    query = """
    SELECT concept_code FROM `$project.dataset.concept`
    WHERE concept_name = '$concept_name' and vocabulary_id = 'PPI' and
    concept_class_id = 'Topic'
    """.replace("$project.dataset", project + "." + dataset).replace(
        "$concept_name", concept_name)
    query_job = big_query_client.query(query)
    return query_job.result()


def open_writers_with_headers(name):
    file_prefix = home_dir + "/" + surveys.get(name)
    controlled_file = open(file_prefix + "_controlled" + ".csv",
                           'w')
    registered_file = open(file_prefix + "_registered" + ".csv",
                           'w')
    all_file = open(file_prefix + "_all" + ".csv", 'w')
    controlled_writer = csv.DictWriter(controlled_file, fieldnames=headers)
    controlled_writer.writeheader()
    registered_writer = csv.DictWriter(registered_file, fieldnames=headers)
    registered_writer.writeheader()
    all_writer = csv.DictWriter(all_file, fieldnames=headersAll)
    all_writer.writeheader()
    return controlled_writer, registered_writer, all_writer, controlled_file, registered_file, all_file


def write_row_all(writer, parent_id, code, name, item_type, min_val, max_val,
    flags, long_code_to_short_code):
    global id_all
    new_row = {'id': id_all, 'parent_id': parent_id, 'code': code,
               'name': name, 'type': item_type, 'min': min_val, 'max': max_val,
               'registered_topic_suppressed': flags[
                   "REGISTERED_TOPIC_SUPPRESSED"],
               'registered_question_suppressed': flags[
                   "REGISTERED_QUESTION_SUPPRESSED"],
               'registered_answer_bucketed': flags[
                   "REGISTERED_ANSWERS_BUCKETED"],
               'registered_answer_suppressed': flags[
                   "REGISTERED_ANSWER_SUPPRESSED"],
               'controlled_topic_suppressed': flags[
                   "CONTROLLED_TOPIC_SUPPRESSED"],
               'controlled_question_suppressed': flags[
                   "CONTROLLED_QUESTION_SUPPRESSED"],
               'controlled_answer_bucketed': flags[
                   "CONTROLLED_ANSWERS_BUCKETED"],
               'controlled_answer_suppressed': flags[
                   "CONTROLLED_ANSWER_SUPPRESSED"]}
    if item_type == 'QUESTION':
        # we have to replace questionCode with short code in Field Annotation
        new_row['code'] = get_short_code(code, long_code_to_short_code)
    writer.writerow(new_row)
    id_all += 1


# builds the OutputControlled/OutputRegistered row
def write_row(writer, parent_id, code, name, item_type, min_value, max_value,
    flags, file_type, long_code_to_short_code):
    global id_controlled
    dict_row_control = {'id': id_controlled, 'parent_id': parent_id,
                        'code': code,
                        'name': name, 'type': item_type, 'min': min_value,
                        'max': max_value,
                        'answers_bucketed': flags[
                            "CONTROLLED_ANSWERS_BUCKETED"]}

    if item_type == 'QUESTION':
        dict_row_control['code'] = get_short_code(code, long_code_to_short_code)
    else:
        dict_row_control['answers_bucketed'] = 0

    global id_registered
    dict_row_register = {'id': id_registered, 'parent_id': parent_id,
                         'code': code,
                         'name': name, 'type': item_type, 'min': min_value,
                         'max': max_value,
                         'answers_bucketed': flags[
                             "REGISTERED_ANSWERS_BUCKETED"]}

    if item_type == 'QUESTION':
        dict_row_register['code'] = get_short_code(code,
                                                   long_code_to_short_code)
    else:
        dict_row_register['answers_bucketed'] = 0

    if file_type == 'controlled':
        writer.writerow(dict_row_control)
        id_controlled += 1
    else:
        writer.writerow(dict_row_register)
        id_registered += 1


def add_all_answers(writer, question_code, choices, parent_id, min_value,
    max_value, flags, long_code_to_short_code):
    if choices != '':

        # ex: COPE_A_43, A lot | COPE_A_3, Somewhat | COPE_A_67, A little | COPE_A_168, Not at all
        answers = choices.replace('|  |', '|').split('|')
        for answer in answers:

            # in the case that there are multiple commas, split at the first instance of a ", "
            item_array = answer.split(",", 1)

            # in the case that we have to replace questionCode with short code in Field Annotation
            answer_code = item_array[0].lower().strip()
            code = get_short_code(answer_code, long_code_to_short_code)

            if question_code == 'overallhealth_averagepain7days':
                for i in range(0, 11):
                    write_row_all(writer, parent_id, None, i
                                  , 'ANSWER', min_value, max_value, flags, None)
                break

            else:
                write_row_all(writer, parent_id, code, item_array[1].strip()
                              , 'ANSWER', min_value, max_value, flags, None)


def add_answers(writer, choices, question_code, parent_id, min_value, max_value,
    flags, file_type,
    long_code_to_short_code, answer_suppression):
    if choices != '':

        # ex: COPE_A_43, A lot | COPE_A_3, Somewhat | COPE_A_67, A little | COPE_A_168, Not at all
        answers = choices.replace('|  |', '|').split('|')
        for answer in answers:

            # in the case that there are multiple commas, split at the first instance of a ", "
            item_array = answer.split(",", 1)

            # in the case that we have to replace questionCode with short code in Field Annotation
            answer_code = item_array[0].lower().strip()
            code = get_short_code(answer_code, long_code_to_short_code)

            if question_code == 'overallhealth_averagepain7days':
                for i in range(0, 11):
                    write_row(writer, parent_id, None, i
                              , 'ANSWER', min_value, max_value, flags,
                              file_type, None)
                break

            # in the case that there are answer_suppressed to write it to appropriate file
            if file_type == 'controlled':
                if answer_suppression.get(
                    "CONTROLLED_ANSWER_SUPPRESSED") is None \
                    or (answer_suppression.get(
                    "CONTROLLED_ANSWER_SUPPRESSED") is not None
                        and code not in answer_suppression.get(
                        "CONTROLLED_ANSWER_SUPPRESSED")):
                    write_row(writer, parent_id, code, item_array[1].strip()
                              , 'ANSWER', min_value, max_value, flags,
                              file_type, None)

            if file_type == 'registered':
                if answer_suppression.get(
                    "REGISTERED_ANSWER_SUPPRESSED") is None \
                    or (answer_suppression.get(
                    "REGISTERED_ANSWER_SUPPRESSED") is not None
                        and code not in answer_suppression.get(
                        "REGISTERED_ANSWER_SUPPRESSED")):
                    write_row(writer, parent_id, code, item_array[1].strip()
                              , 'ANSWER', min_value, max_value, flags,
                              file_type, None)


def add_numeric_answer(writer, parent_id, min_value, max_value, flags,
    file_type):
    # for questions with numeric response, we create an answer row named 'Select a value'
    write_row(writer, parent_id, None, 'Select a value', 'ANSWER', min_value,
              max_value, flags, file_type, None)


def get_short_code(code, long_code_to_short_code):
    if code in long_code_to_short_code:
        code = long_code_to_short_code[code]
    return code


def flush_and_close_files(controlled_file, registered_file, all_file):
    controlled_file.flush()
    controlled_file.close()
    registered_file.flush()
    registered_file.close()
    all_file.flush()
    all_file.close()


if __name__ == '__main__':
    main()
    print("done")
