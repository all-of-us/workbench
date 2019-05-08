const buildQueryConfig = (namedParameter) => {
  if (namedParameter.value) {
    return '      {\n' +
      '        \'name\': "' + namedParameter.value.name + '",\n' +
      '        \'parameterType\': {\'type\': "' + namedParameter.value.parameterType + '"' +
      (namedParameter.value.parameterValue instanceof Object ?
        ',\'arrayType\': {\'type\': "' + namedParameter.value.arrayType + '"},' :
        '') + '},\n' +
      '        \'parameterValue\': {' + ((namedParameter.value.parameterValue instanceof Object) ?
        '\'arrayValues\': [' + namedParameter.value.parameterValue.map(
          npv => '{\'value\': ' + npv.parameterValue + '}') + ']' :
        '\'value\': "' + namedParameter.value.parameterValue + '"') + '}\n' +
      '      },';
  } else {
    return;
  }
};

export const convertQueryToText = (name, domain, query) => {
  const namespace = name.replace(/ /g, '_') + '_' + domain.toString().toLocaleLowerCase() + '_';
  return namespace + 'sql="""' + query.query + '"""\n' +
    namespace + 'query_config = {\n' +
    '  \'query\': {\n' +
    '    \'parameterMode\': \'NAMED\',\n' +
    '    \'queryParameters\': [\n' +
    query.namedParameters.map(np => buildQueryConfig(np) + '\n').join('') +
    '    ]\n' +
    '  }\n' +
    '}\n\n' +
    namespace + 'df = pandas.read_gbq(' + namespace +
      'sql, dialect="standard", configuration=' + namespace + 'query_config)';
};
