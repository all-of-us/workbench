"""Module that loads redcap files into BQ."""

import argparse
import csv
import os
import shutil
from io import StringIO
from os import listdir
from os.path import isfile, join
from google.cloud import bigquery
from google.cloud import storage

# ./project.rb tanagra-stage-redcap-files \
# --project all-of-us-workbench-test --date 2022-09-22 --dataset SR2023Q3R2

# These are redcap files for all surveys except cope
# Note: We only read in the Winter Minute Survey since all prior survey
#   versions are contained in the Winter version. No merge if versioned
#   files is needed, unlike cope surveys
surveys = ['ENGLISHBasics_DataDictionary',
           'ENGLISHLifestyle_DataDictionary',
           'ENGLISHOverallHealth_DataDictionary',
           'ENGLISHHealthCareAccessUtiliza_DataDictionary',
           'ENGLISHNewYearMinuteSurveyOnCO_DataDictionary',
           'ENGLISHSocialDeterminantsOfHea_DataDictionary',
           'ENGLISHPersonalAndFamilyHealth_DataDictionary']

# these are the column names for the controlled and registered output files
headers = ['id', 'concept_code', 'survey_name', 'topic', 'answers']

exclude_list = ["record_id", "_intro", "textbox", "freetext", "_outro",
                "hc_thankyou", "transplantdate", "outro_text", "cope_may",
                "cope_jun", "cope_jul", "cope_codebook_audit",
                "cope_codeversions", "cope_content_tracked", "cope_source_nhs",
                "cope_nov", "cope_octobe_implementation", "october_codes",
                "cope_dec", "cope_feb", "section_participation",
                "section_instructions", "outro_text_2", "cope_documentation",
                "fmh_document", "fmh_codebook", "fmh_concept", "fmh_helptext",
                "fmh_team_feedback", "cdc_covid_xx_a_date1",
                "cdc_covid_xx_b_firstdose_other",
                "cdc_covid_xx_symptom_cope_350",
                "cdc_covid_xx_a_date2", "cdc_covid_xx_b_seconddose_other",
                "cdc_covid_xx_symptom_seconddose_cope_350", "dmfs_29a",
                "dmfs_29_seconddose_other", "cdc_covid_xx_a_date3",
                "cdc_covid_xx_b_dose3_other",
                "cdc_covid_xx_symptom_cope_350_dose3",
                "cdc_covid_xx_type_dose3_other",
                "dmfs_29_additionaldose_other", "cdc_covid_xx_a_date4",
                "cdc_covid_xx_b_dose4_other",
                "cdc_covid_xx_symptom_cope_350_dose4",
                "cdc_covid_xx_type_dose4_other", "cdc_covid_xx_a_date5",
                "cdc_covid_xx_b_dose5_other",
                "cdc_covid_xx_symptom_cope_350_dose5",
                "cdc_covid_xx_type_dose5_other", "cdc_covid_xx_b_dose6_other",
                "cdc_covid_xx_symptom_cope_350_dose6", "cdc_covid_xx_a_date6",
                "cdc_covid_xx_type_dose6_other", "cdc_covid_xx_a_date7",
                "cdc_covid_xx_b_dose7_other",
                "cdc_covid_xx_symptom_cope_350_dose7",
                "cdc_covid_xx_type_dose7_other", "cdc_covid_xx_b_dose8_other",
                "cdc_covid_xx_symptom_cope_350_dose8", "cdc_covid_xx_a_date8",
                "cdc_covid_xx_type_dose8_other",
                "cdc_covid_xx_b_dose9_other", "cdc_covid_xx_a_date9",
                "cdc_covid_xx_symptom_cope_350_dose9",
                "cdc_covid_xx_type_dose9_other", "cdc_covid_xx_a_date10",
                "cdc_covid_xx_b_dose10_other",
                "cdc_covid_xx_symptom_cope_350_dose10",
                "cdc_covid_xx_type_dose10_other", "cdc_covid_xx_a_date11",
                "cdc_covid_xx_b_dose11_other",
                "cdc_covid_xx_symptom_cope_350_dose11",
                "cdc_covid_xx_type_dose11_other", "cdc_covid_xx_b_dose12_other",
                "cdc_covid_xx_symptom_cope_350_dose12", "cdc_covid_xx_a_date12",
                "cdc_covid_xx_type_dose12_other", "cdc_covid_xx_a_date13",
                "cdc_covid_xx_b_dose13_other",
                "cdc_covid_xx_symptom_cope_350_dose13",
                "cdc_covid_xx_type_dose13_other", "cdc_covid_xx_b_dose14_other",
                "cdc_covid_xx_symptom_cope_350_dose14", "cdc_covid_xx_a_date14",
                "cdc_covid_xx_type_dose14_other", "cdc_covid_xx_a_date15",
                "cdc_covid_xx_b_dose15_other",
                "cdc_covid_xx_symptom_cope_350_dose15",
                "cdc_covid_xx_type_dose15_other", "cdc_covid_xx_b_dose16_other",
                "cdc_covid_xx_symptom_cope_350_dose16", "cdc_covid_xx_a_date16",
                "cdc_covid_xx_type_dose16_other", "cdc_covid_xx_a_date17",
                "cdc_covid_xx_b_dose17_other",
                "cdc_covid_xx_symptom_cope_350_dose17",
                "cdc_covid_xx_type_dose17_other"]

HOME_DIR = "../csv"


def main():
    """Main function that creates and inserts redcap data"""
    # parse all provided args
    project, date, dataset = parse_args()

    # validate that all files for a specific date exist in the bucket
    files_exist(date)

    # setup csv directory to hold output csv files
    if os.path.exists(HOME_DIR):
        shutil.rmtree(HOME_DIR)
    os.mkdir(HOME_DIR)

    for name in surveys:
        rows = read_csv(get_filename(name, date))
        file_name = name.replace("ENGLISH", "").replace("_DataDictionary",
                                                        "").lower()
        csv_writer, csv_file = open_writers_with_headers(
            "tanagra_" + file_name + "_staged")
        print("Reading & Parsing... " + name)
        previous_concept_code = None

        index = 1
        for row in rows:
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
                if survey_name == "WinterMinuteSurveyOnCOV":
                    topic = None
                new_entry = {'id': index,
                             'concept_code': concept_code,
                             'survey_name': survey_name,
                             'topic': topic,
                             'answers': answers}
                csv_writer.writerow(new_entry)
                index += 1
            previous_concept_code = concept_code
        flush_and_close_files(csv_file)

    # copy local output files and upload it to the bucket
    storage_client = storage.Client.from_service_account_json(
        '../../sa-key.json')
    bucket = storage_client.get_bucket('all-of-us-workbench-private-cloudsql')
    files = [f for f in listdir(HOME_DIR) if isfile(join(HOME_DIR, f))]
    for f in files:
        blob = bucket.blob(dataset + '/cdr_csv_files/' + f)
        blob.upload_from_filename(HOME_DIR + "/" + f)

    # These files are static and never change moving forward
    bucket.copy_blob(bucket.blob('redcap/tanagra_cope_staged.csv'),
                     bucket,
                     dataset + '/cdr_csv_files/tanagra_cope_staged.csv')

    # when done remove directory and all files
    shutil.rmtree(HOME_DIR)
    
    # Construct a BigQuery client object.
    client = bigquery.Client(project=project)

    # create schema for table
    schema = [
        bigquery.SchemaField("id", "INTEGER", mode="REQUIRED"),
        bigquery.SchemaField("concept_code", "STRING", mode="REQUIRED"),
        bigquery.SchemaField("survey_name", "STRING", mode="REQUIRED"),
        bigquery.SchemaField("topic", "STRING", mode="NULLABLE"),
        bigquery.SchemaField("answers", "STRING", mode="NULLABLE"),
    ]

    table_id = project + "." + dataset + ".prep_redcap_survey_file"

    job_config = bigquery.LoadJobConfig(
        schema=schema,
        skip_leading_rows=1,
        source_format=bigquery.SourceFormat.CSV,
        field_delimiter="|",
    )
    job_config.write_disposition = bigquery.WriteDisposition.WRITE_TRUNCATE
    
    uri = "gs://all-of-us-workbench-private-cloudsql/" + dataset + "/cdr_csv_files/*.csv"
    
    load_job = client.load_table_from_uri(
        uri, table_id, job_config=job_config
    )  # Make an API request.
    
    load_job.result()  # Waits for the job to complete.
    
    destination_table = client.get_table(table_id)  # Make an API request.
    print("Loaded {} rows.".format(destination_table.num_rows))


def parse_args():
    """Parse args"""
    parser = argparse.ArgumentParser(prog='tanagra-stage-redcap-files',
                                     usage='%(prog)s --project <project> --date <date> --dataset <dataset>',
                                     description='Generate csv files')
    parser.add_argument('--project', type=str, help='Source project',
                        required=True)
    parser.add_argument('--date', type=str, help='date for input file',
                        required=True)
    parser.add_argument('--dataset', type=str, help='dataset name',
                        required=True)
    arg = parser.parse_args()
    return arg.project, arg.date, arg.dataset


def read_csv(file):
    """Read csv"""
    blob = get_blob(file)
    blob = blob.download_as_string()
    blob = blob.decode('utf-8-sig')
    blob = StringIO(blob)  # tranform bytes to string
    return csv.DictReader(blob)  # then use csv library to read the content


def files_exist(date):
    """Check if files exist"""
    for name in surveys:
        file = get_filename(name, date)
        blob = get_blob(file)
        if not blob.exists():
            raise Exception(file + " does not exist!")


def get_filename(name, date):
    """Get file name"""
    file = name + '_' + date + '.csv'
    return file


def get_blob(file):
    """Get blob"""
    storage_client = storage.Client.from_service_account_json(
        '../../sa-key.json')
    bucket = storage_client.get_bucket(
        'all-of-us-workbench-private-cloudsql')
    return bucket.blob('redcap/' + file)


def open_writers_with_headers(name):
    """Open writer with headers"""
    dialect = csv.unix_dialect
    dialect.delimiter = "|"
    dialect.quoting = csv.QUOTE_MINIMAL
    file_prefix = HOME_DIR + "/" + name
    csv_file = open(file_prefix + ".csv", 'w', encoding="utf8")
    csv_writer = csv.DictWriter(csv_file, fieldnames=headers, dialect=dialect)
    csv_writer.writeheader()
    return csv_writer, csv_file


def parse_row_columns(row):
    """Parse row columns"""
    concept_code = row['Variable / Field Name']
    topic = row['Section Header'].replace('\n', ' ').replace('"', '')
    answers = row['Choices, Calculations, OR Slider Labels']
    concept_code_rename = row['Field Annotation']
    concept_codes = answers.split(" | ")
    codes_list = list()
    for code in concept_codes:
        codes_list.append(code.split(",")[0])
    return concept_code, topic, " ".join(codes_list), concept_code_rename


def flush_and_close_files(csv_file):
    """Flush and close files"""
    csv_file.flush()
    csv_file.close()


if __name__ == '__main__':
    main()
    print("done")
