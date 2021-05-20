import argparse
import csv
import os
import shutil
from google.cloud import bigquery
from google.cloud import storage
from io import StringIO

# we will increment this as the row number as we write each row in each output file
id_controlled = 1
id_registered = 1
id_all = 1

home_dir = "../csv"

# key = csv prefix, value = csv output file name
surveys = {"ENGLISHBasics_DataDictionary": "Basics",
           "ENGLISHHealthCareAccessUtiliza_DataDictionary": "HealthCareAccessUtiliza",
           "ENGLISHLifestyle_DataDictionary": "Lifestyle",
           "ENGLISHOverallHealth_DataDictionary": "OverallHealth",
           "ENGLISHPersonalMedicalHistory_DataDictionary": "PersonalMedicalHistory"}
# field annotation flags
annotation_flags = {"REGISTERED_TOPIC_SUPPRESSED": "0",
                    "REGISTERED_QUESTION_SUPPRESSED": "0",
                    "REGISTERED_ANSWERS_BUCKETED": "0",
                    "CONTROLLED_TOPIC_SUPPRESSED": "0",
                    "CONTROLLED_QUESTION_SUPPRESSED": "0",
                    "CONTROLLED_ANSWERS_BUCKETED": "0"}
# these are the column names for the controlled and registered output files
headers = ['id', 'parent_id', 'code', 'name', 'type', 'min', 'max',
           'answers_bucketed']
# these are the headers for outputAll file
headersAll = ['id', 'parent_id', 'code', 'name', 'type', 'min', 'max',
              'registered_topic_suppressed', 'registered_question_suppressed',
              'registered_answer_bucketed', 'controlled_topic_suppressed',
              'controlled_question_suppressed', 'controlled_answer_bucketed']

# suppression of certain answers from output file
answerSuppressed = 'CONTROLLED_ANSWER_SUPPRESSED'


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
        print("Reading... " + name)

        # open your file writers for this survey and write headers
        controlled_writer, registered_writer, all_writer = \
            open_writers_with_headers(name)

        # we will use this when the row_number = 1 to add in as the first row
        row_number = 0
        global id_controlled
        global id_registered
        global id_all
        id_controlled = 1
        id_registered = 1
        id_all = 1

        # initializing these variables in the case that the topic is null
        topic_id_all = 0
        topic_id_controlled = 0
        topic_id_register = 0

        for row in rows:

            if not_row_to_skip(row):
                field_annotation, field_annotation_array, question_label, question_code, \
                    text_validation, choices, section_header, min_value, \
                    max_value = parse_row_columns(row)
                # zahra stuff here
                row_number += 1

                item_flags = {}
                for item in annotation_flags:
                    if item in field_annotation:
                        item_flags[item] = 1
                    else:
                        item_flags[item] = 0

                field_names = {}
                for item in field_annotation_array:
                    if item not in annotation_flags:
                        split_codes = item.split('=')
                        if answerSuppressed not in split_codes:
                            # in the case that it doesn't have a code ex: Launched 5/30/2017 (PTSC) & 12/10/2019 (CE).
                            if len(split_codes) > 1:
                                field_names[split_codes[0].strip().lower()] = \
                                    split_codes[1].strip().lower()

                answer_suppression = {}
                for item in field_annotation_array:
                    split = item.split('=')
                    if "CONTROLLED_ANSWER_SUPPRESSED" in split:
                        answer_suppression.setdefault("CONTROLLED_ANSWER_SUPPRESSED", [])
                        answer_suppression["CONTROLLED_ANSWER_SUPPRESSED"].append(split[1].lower())

                # for the first row of survey
                if row_number == 1:
                    survey_id_all = id_all
                    write_row_all(all_writer, 0, question_code, question_label,
                                  'SURVEY', None, None, item_flags, field_names)
                    print("writing in survey row")

                    survey_id_controlled = id_controlled
                    write_row(controlled_writer, 0, question_code, question_label,
                              'SURVEY', None, None, item_flags, 'controlled',
                              field_names)

                    survey_id_registered = id_registered
                    write_row(registered_writer, 0, question_code, question_label,
                              'SURVEY', None, None, item_flags, 'registered',
                              field_names)
                    continue

                ############ Topic ############
                if len(row['Section Header']) > 0:
                    header = row['Section Header']

                    # find the new lines and index to get the header up to the new line
                    # to do: need to figure out what to do with first topic in pers_med_his
                    position = header.find("\n")
                    if position == -1:
                        header = row['Section Header']
                    else:
                        header = row['Section Header'][:position]

                    # get the concept codes for the topics and write topics into file
                    # topic_code = getTopicCode(header.replace("'", "\\'"))
                    topic_codes = query_topic_code(project, dataset, header.replace("'", "\\'"))
                    topic_id_all = id_all
                    topic_code = ''
                    if not topic_codes:
                        topic_code = topic_codes[0]
                    write_row_all(all_writer, survey_id_all, topic_code, header, 'TOPIC', None, None, item_flags,
                                  field_names)
                    print("writing in rows with topics")

                    # in the case that we have topic_suppressed we want to skip that row
                    if item_flags["CONTROLLED_TOPIC_SUPPRESSED"] != 1:
                        topic_id_controlled = id_controlled
                        write_row(controlled_writer, survey_id_controlled, topic_code, header
                                  , 'TOPIC', None, None, item_flags, 'controlled', field_names)

                    if item_flags["REGISTERED_TOPIC_SUPPRESSED"] != 1:
                        topic_id_register = id_registered
                        write_row(registered_writer, survey_id_registered, topic_code, header
                                  , 'TOPIC', None, None, item_flags, 'register', field_names)

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

                # the first row is the survey so we want to look at all other rows
                question_id_all = id_all
                write_row_all(all_writer, parent_id_all, question_code,
                              question_label, 'QUESTION', min_value, max_value,
                              item_flags, field_names)
                print("writing in rows with a question")

                # answers for outputAll file
                add_all_answers(all_writer, question_code choices, question_id_all, None, None, item_flags, field_names)
                print("writing in rows with an answer")

                if item_flags["CONTROLLED_QUESTION_SUPPRESSED"] != 1:
                    question_id_controlled = id_controlled
                    write_row(controlled_writer, parent_id_controlled, question_code, question_label
                              , 'QUESTION', None, None, item_flags, 'controlled', field_names)

                    # for questions with numeric response, we create an answer row named 'Select a value'
                    if is_numeric_answer:
                        add_numeric_answer(controlled_writer, question_id_controlled,
                                           min_value, max_value, item_flags, 'controlled')

                    add_answers(controlled_writer, choices, question_code, question_id_controlled
                                , None, None, item_flags, 'controlled', field_names, answer_suppression)

                if item_flags["REGISTERED_QUESTION_SUPPRESSED"] != 1:
                    question_id_registered = id_registered
                    write_row(registered_writer, parent_id_registered, question_code, question_label
                              , 'QUESTION', None, None, item_flags, 'registered', field_names)

                    if is_numeric_answer:
                        add_numeric_answer(registered_writer, question_id_registered,
                                           min_value, max_value, item_flags, 'registered')

                    add_answers(registered_writer, choices, question_code, question_id_registered
                                , None, None, item_flags, 'registered', field_names, answer_suppression)

    # when done remove directory and all files
#     shutil.rmtree(home_dir)


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


def not_row_to_skip(row):
    return row['Variable / Field Name'].lower() != 'record_id' \
           and row['Field Type'].lower() != 'descriptive' \
           and 'please specify' not in row['Field Label'].lower()


def parse_row_columns(row):
    field_annotation = row['Field Annotation']
    field_annotation_array = []
    if field_annotation != '  ':
        [field_annotation_array.append(item.strip()) for item in
         field_annotation.split(',')]
    # replace all '\n' with empty string
    question_label = row['Field Label'].replace('\n', '')
    question_code = row['Variable / Field Name']
    text_validation = row['Text Validation Type OR Show Slider Number']
    choices = row['Choices, Calculations, OR Slider Labels']
    section_header = row['Section Header']
    min_value = row['Text Validation Min']
    max_value = row['Text Validation Max']
    return field_annotation, field_annotation_array, question_label, question_code, \
           text_validation, choices, section_header, min_value, max_value


def get_blob(file):
    storage_client = storage.Client.from_service_account_json(
        '../../sa-key.json')
    bucket = storage_client.get_bucket(
        'all-of-us-workbench-private-cloudsql')
    return bucket.blob('cb_prep_tables/redcap/' + file)


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
    csv_controlled = open(file_prefix + "_controlled.csv", 'w')
    csv_registered = open(file_prefix + "_registered.csv", 'w')
    csv_all = open(file_prefix + "_all.csv", 'w')
    controlled_writer = csv.DictWriter(csv_controlled, fieldnames=headers)
    controlled_writer.writeheader()
    registered_writer = csv.DictWriter(csv_registered, fieldnames=headers)
    registered_writer.writeheader()
    all_writer = csv.DictWriter(csv_all, fieldnames=headersAll)
    all_writer.writeheader()
    return controlled_writer, registered_writer, all_writer


def write_row_all(writer, parent_id, code, name, item_type, min_val, max_val,
                  flags, field_names):
    global id_all
    new_row = {'id': id_all, 'parent_id': parent_id, 'code': code,
               'name': name, 'type': item_type, 'min': min_val, 'max': max_val,
               'registered_topic_suppressed': flags[
                   "REGISTERED_TOPIC_SUPPRESSED"],
               'registered_question_suppressed': flags[
                   "REGISTERED_QUESTION_SUPPRESSED"],
               'registered_answer_bucketed': flags[
                   "REGISTERED_ANSWERS_BUCKETED"],
               'controlled_topic_suppressed': flags[
                   "CONTROLLED_TOPIC_SUPPRESSED"],
               'controlled_question_suppressed': flags[
                   "CONTROLLED_QUESTION_SUPPRESSED"],
               'controlled_answer_bucketed': flags[
                   "CONTROLLED_ANSWERS_BUCKETED"]}
    if item_type == 'QUESTION':
        # we have to replace questionCode with short code in Field Annotation
        new_row['code'] = get_short_code(code, field_names)

    writer.writerow(new_row)
    id_all += 1


# builds the OutputControlled/OutputRegistered row that will get passed through the addRow function
def write_row(writer, parent_id, code, name, item_type, min_value, max_value,
              flags, file_type, field_names):
    global id_controlled
    dict_row_control = {'id': id_controlled, 'parent_id': parent_id, 'code': code,
                        'name': name, 'type': item_type, 'min': min_value, 'max': max_value,
                        'answers_bucketed': flags["CONTROLLED_ANSWERS_BUCKETED"]}

    if item_type == 'QUESTION':
        dict_row_control['code'] = get_short_code(code, field_names)

    global id_registered
    dict_row_register = {'id': id_registered, 'parent_id': parent_id,
                         'code': code,
                         'name': name, 'type': item_type, 'min': min_value, 'max': max_value,
                         'answers_bucketed': flags["REGISTERED_ANSWERS_BUCKETED"]}

    if item_type == 'QUESTION':
        dict_row_register['code'] = get_short_code(code, field_names)

    if file_type == 'controlled':
        writer.writerow(dict_row_control)
        id_controlled += 1
    else:
        writer.writerow(dict_row_register)
        id_registered += 1


def add_all_answers(writer, question_code, choices, parent_id, min_value, max_value, flags, field_names):
    if choices != '':
        # ex: COPE_A_43, A lot | COPE_A_3, Somewhat | COPE_A_67, A little | COPE_A_168, Not at all
        answers = choices.replace('|  |', '|').split('|')
        for answer in answers:

            # in the case that there are multiple commas, split at the first instance of a ", "
            item_array = answer.split(",", 1)

            # in the case that we have to replace questionCode with short code in Field Annotation
            answer_code = item_array[0].lower().strip()
            code = get_short_code(answer_code, field_names)

            if question_code == 'overallhealth_averagepain7days':
                for i in range(0, 11):
                    write_row_all(writer, parent_id, None, i
                                  , 'ANSWER', min_value, max_value, flags, None)
                break

            else:
                write_row_all(writer, parent_id, code, item_array[1].strip()
                              , 'ANSWER', min_value, max_value, flags, None)


def add_answers(writer, choices, question_code, parent_id, min_value, max_value, flags, file_type, field_names,
                answer_suppression):
    if choices != '':
        # ex: COPE_A_43, A lot | COPE_A_3, Somewhat | COPE_A_67, A little | COPE_A_168, Not at all
        answers = choices.replace('|  |', '|').split('|')
        for answer in answers:
            # in the case that there are multiple commas, split at the first instance of a ", "
            item_array = answer.split(",", 1)

            # in the case that we have to replace questionCode with short code in Field Annotation
            answer_code = item_array[0].lower().strip()
            # print(answer_code)
            code = get_short_code(answer_code, field_names)
            # change to questionCode
            if question_code == 'overallhealth_averagepain7days':
                for i in range(0, 11):
                    write_row(writer, parent_id, None, i
                              , 'ANSWER', min_value, max_value, flags, file_type, None)
                break

            elif answer_suppression.get("CONTROLLED_ANSWER_SUPPRESSED") is not None:
                if answer_code not in answer_suppression.get("CONTROLLED_ANSWER_SUPPRESSED"):
                    write_row(writer, parent_id, code, item_array[1].strip()
                              , 'ANSWER', min_value, max_value, flags, file_type, None)

            else:
                write_row(writer, parent_id, code, item_array[1].strip()
                          , 'ANSWER', min_value, max_value, flags, file_type, None)


def add_numeric_answer(writer, parent_id, min_value, max_value, flags, file_type):
    # for questions with numeric response, we create an answer row named 'Select a value'

    write_row(writer, parent_id, None, 'Select a value', 'ANSWER', min_value, max_value, flags, file_type, None)


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
