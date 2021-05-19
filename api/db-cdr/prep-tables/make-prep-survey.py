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

        # open your file writers for this survey and write headers
        controlledWriter, registeredWriter, allWriter = \
            open_writers_with_headers(name)

        # we will use this when the rowNumber = 1 to add in as the first row
        rowNumber = 0
        global id_controlled
        global id_registered
        global id_all
        id_controlled = 1
        id_registered = 1
        id_all = 1

        # initializing these variables in the case that the topic is null
        topicIdAll = 0
        topicIdControlled = 0
        topicIdRegister = 0

        for row in rows:

            if not_row_to_skip(row):
                field_annotation, field_annotation_array, question_label, question_code, \
                text_validation, choices, section_header, min_value, \
                max_value = parse_row_columns(row)
                # zahra stuff here
                rowNumber += 1

                itemFlags = {}
                for item in annotation_flags:
                    if item in field_annotation:
                        itemFlags[item] = 1
                    else:
                        itemFlags[item] = 0

                fieldNames = {}
                for item in field_annotation_array:
                    if item not in annotation_flags:
                        splitCodes = item.split('=')
                        if answerSuppressed not in splitCodes:
                            # in the case that it doesn't have a code ex: Launched 5/30/2017 (PTSC) & 12/10/2019 (CE).
                            if len(splitCodes) > 1:
                                fieldNames[splitCodes[0].strip().lower()] = \
                                    splitCodes[1].strip().lower()

                # for the first row of survey
                if rowNumber == 1:
                    surveryIdAll = id_all
                    writeRowAll(allWriter, 0, question_code, question_label,
                                'SURVEY', None, None, itemFlags, fieldNames)

                    surveryIdControlled = id_controlled
                    writeRow(controlledWriter, 0, question_code, question_label,
                             'SURVEY', None, None, itemFlags, 'controlled',
                             fieldNames)

                    surveryIdRegistered = id_registered
                    writeRow(registeredWriter, 0, question_code, question_label,
                             'SURVEY', None, None, itemFlags, 'registered',
                             fieldNames)
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
                    #topicCode = getTopicCode(header.replace("'", "\\'"))
                    topicCodes = query_topic_code(project, dataset, header.replace("'", "\\'"))
                    for topicCode in topicCodes:
                        print(topicCode.concept_code)
                        topicIdAll = id_all
                        writeRowAll(allWriter,surveryIdAll, topicCode.concept_code, header,'TOPIC', None, None, itemFlags,fieldNames)

                    # in the case that we have topic_suppressed we want to skip that row
                    # if itemFlags["CONTROLLED_TOPIC_SUPPRESSED"] != 1:
                    #     topicIdControlled = idControlled
                    #     writeRow(controlledWriter,surveryIdControlled, topicCode, header
                    #                 ,'TOPIC', None, None, itemFlags,'controlled',fieldNames)
                    #
                    # if itemFlags["REGISTERED_TOPIC_SUPPRESSED"] != 1:
                    #     topicIdRegister = idRegistered
                    #     writeRow(registeredWriter,surveryIdRegistered, topicCode, header
                    #              ,'TOPIC', None, None,itemFlags,'register',fieldNames)

                 ############ Question and Answer ############
                 # if there is a topic use it otherwise use the survey as parent_id
                parentIdAll = surveryIdAll if topicIdAll == 0 else topicIdAll
                parentIdControlled = surveryIdControlled if topicIdControlled == 0 else topicIdControlled
                parentIdRegistered = surveryIdRegistered if topicIdRegister == 0 else topicIdRegister

                minValue = ''
                maxValue = ''
                isNumericAnswer = False

                if (
                    text_validation == 'integer' or text_validation == 'number') and \
                    row['Text Validation Min'] and row[
                    'Text Validation Max'] != '':
                    minValue = row['Text Validation Min']
                    maxValue = row['Text Validation Max']
                    isNumericAnswer = True

                # the first row is the survey so we want to look at all other rows
                questionIdAll = id_all
                writeRowAll(allWriter, parentIdAll, question_code,
                            question_label, 'QUESTION', minValue, maxValue,
                            itemFlags, fieldNames)

                # answers for outputAll file
    #                 addAllAnswers(allFile,choices, questionIdAll, None, None, itemFlags,fieldNames)

    # This will query the concept table using concept_name
    results = query_topic_code(project, dataset, "Respiratory Conditions")
    for result in results:
        print(result.concept_code)

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
    controlledWriter = csv.DictWriter(csv_controlled, fieldnames=headers)
    controlledWriter.writeheader()
    registeredWriter = csv.DictWriter(csv_registered, fieldnames=headers)
    registeredWriter.writeheader()
    allWriter = csv.DictWriter(csv_all, fieldnames=headersAll)
    allWriter.writeheader()
    return controlledWriter, registeredWriter, allWriter


def writeRowAll(writer, parent_id, code, name, item_type, min_val, max_val,
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
def writeRow(writer, parent_id, code, name, item_type, minValue, maxValue,
    flags, fileType, field_names):
    global id_controlled
    dictRowControl = {'id': id_controlled, 'parent_id': parent_id, 'code': code,
                      'name': name, 'type': item_type, 'min': minValue
        , 'max': maxValue,
                      'answers_bucketed': flags["CONTROLLED_ANSWERS_BUCKETED"]}

    if item_type == 'QUESTION':
        dictRowControl['code'] = get_short_code(code, field_names)

    global id_registered
    dictRowRegister = {'id': id_registered, 'parent_id': parent_id,
                       'code': code,
                       'name': name, 'type': item_type, 'min': minValue
        , 'max': maxValue,
                       'answers_bucketed': flags["REGISTERED_ANSWERS_BUCKETED"]}

    if item_type == 'QUESTION':
        dictRowRegister['code'] = get_short_code(code, field_names)

    if fileType == 'controlled':
        writer.writerow(dictRowControl)
        id_controlled += 1
    else:
        writer.writerow(dictRowRegister)
        id_registered += 1


# def addAllAnswers(writer,choices,parent_id, minValue, maxValue,flags,fieldNames):
#     if choices != '':
#         # ex: COPE_A_43, A lot | COPE_A_3, Somewhat | COPE_A_67, A little | COPE_A_168, Not at all
#         answers = choices.replace('|  |', '|').split('|')
#         for answer in answers:
#
#             # in the case that there are multiple commas, split at the first instance of a ", "
#             itemArray = answer.split(",", 1)
#
#             # in the case that we have to replace questionCode with short code in Field Annotation
#             answerCode = itemArray[0].lower().strip()
#             code = get_short_code(answerCode,fieldNames)
#
#             if answerCode == '0 (no pain)' or answerCode == '10 (worst pain imaginable)':
#                 for i in range(0,11):
#                     buildRowAll(file,parent_id, None, i
#                                     , 'ANSWER', minValue, maxValue,itemFlags,None)
#                 break
#
#             else:
#                 buildRowAll(file,parent_id, code, itemArray[1].strip()
#                                        , 'ANSWER', minValue, maxValue,itemFlags,None)

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
