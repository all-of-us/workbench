import argparse
import csv
import os
import shutil
from collections import OrderedDict
from io import StringIO
from os import listdir
from os.path import isfile, join

from google.cloud import storage

# these are the column names for the controlled and registered output files
surveys = ['ENGLISHBasics_DataDictionary',
           'ENGLISHLifestyle_DataDictionary',
           'ENGLISHOverallHealth_DataDictionary',
           'ENGLISHPersonalMedicalHistory_DataDictionary',
           'FamilyHealthHistory',
           'ENGLISHHealthCareAccessUtiliza_DataDictionary',
           'ENGLISHMay2020Covid19Participa_DataDictionary',
           'ENGLISHJune2020Covid19Particip_DataDictionary',
           'ENGLISHJuly2020Covid19Particip_DataDictionary',
           'ENGLISHNovember2020COVID19Part_DataDictionary',
           'ENGLISHDecember2020COVID19Part_DataDictionary',
           'ENGLISHFebruary2021COVID19Part_DataDictionary']

# these are the column names for the controlled and registered output files
headers = ['concept_code', 'survey_name', 'topic', 'answers']

exclude_list = ["record_id", "_intro", "textbox", "freetext", "_outro",
                "hc_thankyou", "transplantdate", "outro_text", "cope_may",
                "cope_jun", "cope_jul", "cope_codebook_audit",
                "cope_codeversions", "cope_content_tracked", "cope_source_nhs",
                "cope_nov", "cope_octobe_implementation", "october_codes",
                "cope_dec", "cope_feb", "section_participation",
                "section_instructions", "outro_text_2", "cope_documentation",
                "fmh_document", "fmh_codebook", "fmh_concept", "fmh_helptext",
                "fmh_team_feedback"]

home_dir = "../csv"


def main():
    # parse all provided args
    date, dataset = parse_args()

    # validate that all files for a specific date exist in the bucket
    files_exist(date)

    # setup csv directory to hold output csv files
    if os.path.exists(home_dir):
        shutil.rmtree(home_dir)
    os.mkdir(home_dir)

    # open your file writers for this survey and write headers
    csv_writer, csv_file = open_writers_with_headers("prep_survey_staged")
    master_cope_dictionary = OrderedDict()

    for name in surveys:
        rows = read_csv(get_filename(name, date))
        print("Reading & Parsing... " + name)
        previous_concept_code = None

        for row in rows:
            if name == "FamilyHealthHistory":
                concept_code, survey_name, topic, answers = parse_family_health_row_columns(
                    row)
                new_entry = {'concept_code': concept_code,
                             'survey_name': survey_name,
                             'topic': topic,
                             'answers': answers}
                csv_writer.writerow(new_entry)
            else:
                concept_code, topic, answers, concept_code_rename = \
                    parse_row_columns(row)
                if concept_code_rename:
                    possible_names = concept_code_rename.split(",")
                    for possible_name in possible_names:
                        if possible_name.strip().startswith(concept_code):
                            new_name = possible_name.split("=").pop(1)
                            if new_name and previous_concept_code != new_name:
                                concept_code = new_name
                if not list(filter(concept_code.endswith, exclude_list)):
                    survey_name = name.replace("ENGLISH", "") \
                        .replace("_DataDictionary", "")
                    new_entry = {'concept_code': concept_code,
                                 'survey_name': survey_name,
                                 'topic': topic,
                                 'answers': answers}
                    if "covid" not in name.lower():
                        csv_writer.writerow(new_entry)
                    else:
                        if "may2020" in name.lower():
                            if len(master_cope_dictionary) == 0:
                                cope_entry = {'concept_code': 'cope',
                                              'survey_name': survey_name,
                                              'topic': None,
                                              'answers': None}
                                master_cope_dictionary['cope'] = cope_entry
                            master_cope_dictionary[concept_code] = new_entry
                        else:
                            if master_cope_dictionary.get(concept_code) is None:
                                if concept_code == "c19corset_59":
                                    previous_concept_code = "ukmh_j3"
                                master_cope_dictionary = rewrite_dictionary(
                                    master_cope_dictionary,
                                    previous_concept_code,
                                    new_entry)
                            else:
                                master_cope_dictionary[concept_code] = new_entry
                previous_concept_code = concept_code

    for key in master_cope_dictionary:
        csv_writer.writerow(master_cope_dictionary.get(key))

    flush_and_close_files(csv_file)

    # copy local output files and upload it to the bucket
    storage_client = storage.Client.from_service_account_json(
        '../../sa-key.json')
    bucket = storage_client.get_bucket('all-of-us-workbench-private-cloudsql')
    files = [f for f in listdir(home_dir) if isfile(join(home_dir, f))]
    for f in files:
        blob = bucket.blob(dataset + '/cdr_csv_files/' + f)
        blob.upload_from_filename(home_dir + "/" + f)

    # when done remove directory and all files
    shutil.rmtree(home_dir)


def parse_args():
    parser = argparse.ArgumentParser(prog='stage-redcap-files',
                                     usage='%(prog)s --date <date>',
                                     description='Generate csv files')
    parser.add_argument('--date', type=str, help='date for input file',
                        required=True)
    parser.add_argument('--dataset', type=str, help='dataset name',
                        required=True)
    arg = parser.parse_args()
    return arg.date, arg.dataset


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
    if name == "FamilyHealthHistory":
        file = "FamilyHealthHistory.csv"
    return file


def get_blob(file):
    storage_client = storage.Client.from_service_account_json(
        '../../sa-key.json')
    bucket = storage_client.get_bucket(
        'all-of-us-workbench-private-cloudsql')
    return bucket.blob('redcap/' + file)


def open_writers_with_headers(name):
    dialect = csv.unix_dialect
    dialect.delimiter = "|"
    dialect.quoting = csv.QUOTE_MINIMAL
    file_prefix = home_dir + "/" + name
    csv_file = open(file_prefix + ".csv", 'w')
    csv_writer = csv.DictWriter(csv_file, fieldnames=headers, dialect=dialect)
    csv_writer.writeheader()
    return csv_writer, csv_file


def parse_row_columns(row):
    concept_code = row['Variable / Field Name']
    topic = row['Section Header'].replace('\n', ' ').replace('"', '')
    answers = row['Choices, Calculations, OR Slider Labels']
    concept_code_rename = row['Field Annotation']
    concept_codes = answers.split(" | ")
    codes_list = list()
    for code in concept_codes:
        codes_list.append(code.split(",")[0])
    return concept_code, topic, " ".join(codes_list), concept_code_rename


def parse_family_health_row_columns(row):
    concept_code = row['concept_code']
    survey_name = row['survey_name']
    topic = row['topic']
    answers = row['answers']
    return concept_code, survey_name, topic, answers


def flush_and_close_files(csv_file):
    csv_file.flush()
    csv_file.close()


def rewrite_dictionary(master_dictionary, previous_key, new_entry):
    new_dictionary = OrderedDict()
    for key in master_dictionary:
        new_dictionary[master_dictionary.get(key)[
            'concept_code']] = master_dictionary.get(key)
        if previous_key == key:
            new_dictionary[new_entry.get('concept_code')] = new_entry
    return new_dictionary


if __name__ == '__main__':
    main()
    print("done")
