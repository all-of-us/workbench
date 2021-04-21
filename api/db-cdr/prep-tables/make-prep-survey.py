import csv


def main():
    # Get the BigQuery curated dataset for the current workspace context.
    CDR = args.project + "." + args.dataset
    # CHANGE THIS PER SURVEY
    fileName = "thebasics_updated"
    inputs = fileName + ".csv"

    outputAll = fileName + "_all.csv"
    outputRegistered = fileName + "_registered.csv"
    outputControlled = fileName + "_controlled.csv"

    # we will increment this as the row number as we write each row in each output file
    idNumControlled = 1
    idNumRegistered = 1
    idNumAll = 1

    # created an array of Field Names to skip and can add to this if needed
    skipFieldNames = ['record_id']

    # these are the column names that we are writing in the controlled and registered output files
    headers = ['id', 'parent_id', 'code', 'name', 'type', 'min', 'max',
               'answers_bucketed']

    # these are the headers for outputAll file
    headersAll = ['id', 'parent_id', 'code', 'name', 'type', 'min', 'max'
        , 'registered_topic_suppressed', 'registered_question_suppressed',
                  'registered_answer_bucketed'
        , 'controlled_topic_suppressed', 'controlled_question_suppressed',
                  'controlled_answer_bucketed']

    # these are the flags that we will look for in the original file
    possibleFlags = ['REGISTERED_QUESTION_SUPPRESSED',
                     'REGISTERED_TOPIC_SUPPRESSED',
                     'REGISTERED_ANSWERS_BUCKETED'
        , 'CONTROLLED_QUESTION_SUPPRESSED', 'CONTROLLED_TOPIC_SUPPRESSED',
                     'CONTROLLED_ANSWERS_BUCKETED']

    with open(inputs, newline='', encoding='utf-8') as csvfile:
        reader = csv.DictReader(csvfile)
        csvFileControlled = open(outputControlled, 'w')
        csvFileRegistered = open(outputRegistered, 'w')
        csvFileAll = open(outputAll, 'w')

        controlledFile = csv.DictWriter(csvFileControlled, fieldnames=headers)
        controlledFile.writeheader()

        registeredFile = csv.DictWriter(csvFileRegistered, fieldnames=headers)
        registeredFile.writeheader()

        allFile = csv.DictWriter(csvFileAll, fieldnames=headersAll)
        allFile.writeheader()

        # initializing these variables in the case that the topic is null
        topicIdAll = 0
        topicIdControlled = 0
        topicIdRegister = 0

        # we will use this when the rowNumber = 1 to add in as the first row
        rowNumber = 0
        for row in reader:
            if row['Variable / Field Name'] not in skipFieldNames and row[
                'Field Type'].lower() != 'descriptive' and row[
                'Field Label'].lower().find('please specify') == -1:

                # metadata from redcap
                fieldAnnotation = row['Field Annotation']
                fieldAnnotationArray = []
                if fieldAnnotation != '  ':
                    [fieldAnnotationArray.append(item.strip()) for item in
                     fieldAnnotation.split(',')]

                    questionLabel = row['Field Label'].replace('\n',
                                                               '')  # replace all '\n' with empty string
                    questionCode = row['Variable / Field Name']
                    textValidation = row[
                        'Text Validation Type OR Show Slider Number']
                    choices = row['Choices, Calculations, OR Slider Labels']
                    rowNumber += 1

                    itemFlags = {}
                    for item in possibleFlags:
                        if item in fieldAnnotation:
                            itemFlags[item] = 1
                        else:
                            itemFlags[item] = 0

                    # in the case that fieldAnnotationsArray contain long code = short code
                    fieldNames = {}
                    for item in fieldAnnotationArray:
                        if item not in possibleFlags:
                            splitCodes = item.split('=')
                            # in the case that it doesn't have a code ex: Launched 5/30/2017 (PTSC) & 12/10/2019 (CE).
                            if len(splitCodes) > 1:
                                fieldNames[splitCodes[0].strip().lower()] = \
                                    splitCodes[
                                        1].strip().lower()

                        # for the first row of survey
                        if rowNumber == 1:
                            surveryIdAll = idNumAll
                            buildRowAll(allFile, 0, questionCode, questionLabel,
                                        'SURVEY',
                                        None, None, None, None)

                            surveryIdControlled = idNumControlled
                            buildRow(controlledFile, 0, questionCode,
                                     questionLabel, 'SURVEY',
                                     None, None, None, 'controlled', None)

                            surveryIdRegistered = idNumRegistered
                            buildRow(registeredFile, 0, questionCode,
                                     questionLabel, 'SURVEY',
                                     None, None, None, 'registered', None)
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
                            topicCode = getTopicCode(header.replace("'", "\\'"))
                            topicIdAll = idNumAll
                            buildRowAll(allFile, surveryIdAll, topicCode,
                                        header, 'TOPIC',
                                        None, None, itemFlags, fieldNames)

                            # in the case that we have topic_suppressed we want to skip that row
                            if itemFlags["CONTROLLED_TOPIC_SUPPRESSED"] != 1:
                                topicIdControlled = idNumControlled
                                buildRow(controlledFile, surveryIdControlled,
                                         topicCode, header,
                                         'TOPIC', None, None, itemFlags,
                                         'controlled',
                                         fieldNames)

                                if itemFlags[
                                    "REGISTERED_TOPIC_SUPPRESSED"] != 1:
                                    topicIdRegister = idNumRegistered
                                    buildRow(registeredFile,
                                             surveryIdRegistered, topicCode,
                                             header, 'TOPIC', None, None,
                                             itemFlags, 'register',
                                             fieldNames)

                        ############ Question and Answer ############
                        # if there is a topic use it otherwise use the survey as parent_id
                        parentIdAll = surveryIdAll if topicIdAll == 0 else topicIdAll
                        parentIdControlled = surveryIdControlled if topicIdControlled == 0 else topicIdControlled
                        parentIdRegistered = surveryIdRegistered if topicIdRegister == 0 else topicIdRegister

                        minValue = ''
                        maxValue = ''
                        isNumericAnswer = False

                        if (
                            textValidation == 'integer' or textValidation == 'number') and \
                            row['Text Validation Min'] and row[
                            'Text Validation Max'] != '':
                            minValue = row['Text Validation Min']
                            maxValue = row['Text Validation Max']
                            isNumericAnswer = True

                        # the first row is the survey so we want to look at all other rows
                        questionIdAll = idNumAll
                        buildRowAll(allFile, parentIdAll, questionCode,
                                    questionLabel,
                                    'QUESTION', minValue, maxValue, itemFlags,
                                    fieldNames)

                        # answers for outputAll file
                        addAllAnswers(allFile, choices, questionIdAll, None,
                                      None,
                                      itemFlags, fieldNames)

                        if itemFlags["CONTROLLED_QUESTION_SUPPRESSED"] != 1:
                            questionIdControlled = idNumControlled
                            buildRow(controlledFile, parentIdControlled,
                                     questionCode,
                                     questionLabel, 'QUESTION', None, None,
                                     itemFlags,
                                     'controlled', fieldNames)

                            # for questions with numeric response, we create an answer row named 'Select a value'
                            if isNumericAnswer:
                                addNumericAnswer(controlledFile,
                                                 questionIdControlled, minValue,
                                                 maxValue, itemFlags,
                                                 'controlled')

                                addAnswers(controlledFile, choices,
                                           questionIdControlled, None,
                                           None, itemFlags, 'controlled',
                                           fieldNames)

                        if itemFlags["REGISTERED_QUESTION_SUPPRESSED"] != 1:
                            questionIdRegistered = idNumRegistered
                            buildRow(registeredFile, parentIdRegistered,
                                     questionCode,
                                     questionLabel, 'QUESTION', None, None,
                                     itemFlags,
                                     'registered', fieldNames)

                            if isNumericAnswer:
                                addNumericAnswer(registeredFile,
                                                 questionIdRegistered, minValue,
                                                 maxValue, itemFlags,
                                                 'registered')
                                addAnswers(registeredFile, choices,
                                           questionIdRegistered, None,
                                           None, itemFlags, 'registered',
                                           fieldNames)


# adds row to file of choice
def addRow(file, dictRow):
    file.writerow(dictRow)


def getShortCode(code, fieldNames):
    for item in fieldNames:
        if code in fieldNames:
            code = fieldNames[code]
        else:
            code
        return code


# builds the OutputAll row for each topic which gets passed through the addRow function
def buildRowAll(file, parentId, code, name, itemType, minValue, maxValue,
    itemFlags, fieldNames):
    global idNumAll
    dictRow = {'id': idNumAll, 'parent_id': parentId, 'code': code,
               'name': name,
               'type': itemType, 'min': minValue, 'max': maxValue}

    if itemType == 'TOPIC':
        dictRow['registered_topic_suppressed'] = itemFlags[
            "REGISTERED_TOPIC_SUPPRESSED"]
        dictRow['registered_question_suppressed'] = '0'
        dictRow['registered_answer_bucketed'] = '0'
        dictRow['controlled_topic_suppressed'] = itemFlags[
            "CONTROLLED_TOPIC_SUPPRESSED"]
        dictRow['controlled_question_suppressed'] = '0'
        dictRow['controlled_answer_bucketed'] = '0'

    elif itemType == 'QUESTION':
        # in the case that we have to replace questionCode with short code in Field Annotation
        questionShortCode = getShortCode(code, fieldNames)
        dictRow['code'] = questionShortCode
        dictRow['registered_topic_suppressed'] = '0'
        dictRow['registered_question_suppressed'] = itemFlags[
            "REGISTERED_QUESTION_SUPPRESSED"]
        dictRow['registered_answer_bucketed'] = itemFlags[
            "REGISTERED_ANSWERS_BUCKETED"]
        dictRow['controlled_topic_suppressed'] = '0'
        dictRow['controlled_question_suppressed'] = itemFlags[
            "CONTROLLED_QUESTION_SUPPRESSED"]
        dictRow['controlled_answer_bucketed'] = itemFlags[
            "CONTROLLED_ANSWERS_BUCKETED"]

    else:  # survey/answer
        dictRow['registered_topic_suppressed'] = '0'
        dictRow['registered_question_suppressed'] = '0'
        dictRow['registered_answer_bucketed'] = '0'
        dictRow['controlled_topic_suppressed'] = '0'
        dictRow['controlled_question_suppressed'] = '0'
        dictRow['controlled_answer_bucketed'] = '0'

    addRow(file, dictRow)
    idNumAll += 1


# builds the OutputControlled/OutputRegistered row that will get passed through the addRow function
def buildRow(file, parent_id, code, name, itemType, minValue, maxValue,
    itemFlags, fileType, fieldNames):
    global idNumControlled
    dictRowControl = {'id': idNumControlled, 'parent_id': parent_id,
                      'code': code,
                      'name': name, 'type': itemType, 'min': minValue,
                      'max': maxValue}

    if itemType == 'TOPIC':
        dictRowControl['answers_bucketed'] = 0

    elif itemType == 'QUESTION':
        questionShortCode = getShortCode(code, fieldNames)
        dictRowControl['code'] = questionShortCode
        dictRowControl['answers_bucketed'] = itemFlags[
            "CONTROLLED_ANSWERS_BUCKETED"]

    else:
        dictRowControl['answers_bucketed'] = 0

    global idNumRegistered
    dictRowRegister = {'id': idNumRegistered, 'parent_id': parent_id,
                       'code': code, 'name': name, 'type': itemType,
                       'min': minValue, 'max': maxValue}

    if itemType == 'TOPIC':
        dictRowRegister['answers_bucketed'] = 0

    elif itemType == 'QUESTION':
        questionShortCode = getShortCode(code, fieldNames)
        dictRowRegister['code'] = questionShortCode
        dictRowRegister['answers_bucketed'] = itemFlags[
            "REGISTERED_ANSWERS_BUCKETED"]
    else:
        dictRowRegister['answers_bucketed'] = 0

    if fileType == 'controlled':
        addRow(file, dictRowControl)
        idNumControlled += 1
    else:
        addRow(file, dictRowRegister)
        idNumRegistered += 1


def addNumericAnswer(file, parent_id, minValue, maxValue, itemFlags, fileType):
    # for questions with numeric response, we create an answer row named 'Select a value'
    buildRow(file, parent_id, None, 'Select a value', 'ANSWER', minValue,
             maxValue, itemFlags, fileType, None)


def addAllAnswers(file, choices, parent_id, minValue, maxValue, itemFlags,
    fieldNames):
    if choices != '':

        # ex: COPE_A_43, A lot | COPE_A_3, Somewhat | COPE_A_67, A little | COPE_A_168, Not at all
        answers = choices.replace('|  |', '|').split('|')

        # print(len(answers))
        for answer in answers:
            # in the case that there are multiple commas, split at the first instance of a ", "
            itemArray = answer.split(",", 1)

            # in the case that we have to replace questionCode with short code in Field Annotation
            answerCode = itemArray[0].lower().strip()
            code = getShortCode(answerCode, fieldNames)

            if answerCode == '0 (no pain)' or answerCode == '10 (worst pain imaginable)':
                for i in range(0, 11):
                    buildRowAll(file, parent_id, None, i, 'ANSWER', minValue,
                                maxValue,
                                itemFlags, None)
                    break

            else:
                buildRowAll(file, parent_id, code, itemArray[1].strip(),
                            'ANSWER',
                            minValue, maxValue, itemFlags, None)


def addAnswers(file, choices, parent_id, minValue, maxValue, itemFlags,
    fileType, fieldNames):
    if choices != '':
        # ex: COPE_A_43, A lot | COPE_A_3, Somewhat | COPE_A_67, A little | COPE_A_168, Not at all
        answers = choices.replace('|  |', '|').split('|')
        for answer in answers:
            # in the case that there are multiple commas, split at the first instance of a ", "
            itemArray = answer.split(",", 1)

            # in the case that we have to replace questionCode with short code in Field Annotation
            answerCode = itemArray[0].lower().strip()
            code = getShortCode(answerCode, fieldNames)

            if answerCode == '0 (no pain)' or answerCode == '10 (worst pain imaginable)':
                for i in range(0, 11):
                    buildRow(file, parent_id, None, i, 'ANSWER', minValue,
                             maxValue,
                             itemFlags, fileType, None)
                    break

            else:
                buildRow(file, parent_id, code, itemArray[1].strip(), 'ANSWER',
                         minValue, maxValue, itemFlags, fileType, None)


# obtaining the concept code for topics
def getTopicCode(topicName):
    bigQueryClient = bigquery.Client()
    QUERY = (
        'SELECT concept_code AS sectionHeader FROM `{CDR}.concept` WHERE concept_name = "{topicName}" and vocabulary_id = "PPI" and concept_class_id = "Topic"')
    query_job = bigQueryClient.query(QUERY)
    rows = query_job.result()
    for row in rows:
        print(row.sectionHeader)
    # in order to get the concept_code run query


#   topicCode = pd.io.gbq.read_gbq(f'''
#       SELECT concept_code AS sectionHeader
#       FROM `{CDR}.concept`
#       WHERE concept_name = '{topicName}'
#         and vocabulary_id = 'PPI'
#         and concept_class_id = 'Topic'
#       ''', dialect='standard')
#
#   return '' if topicCode.empty else topicCode['sectionHeader'].iloc[0]

if __name__ == '__main__':
    main()
    print("done")
