import { serverConfigStore } from 'app/utils/stores';

export function getDiseaseNames(keyword: string): Promise<Array<string>> {
  const baseurl = serverConfigStore.get().config.firecloudURL;
  const url = baseurl + '/duos/autocomplete/' + keyword;
  return fetch(encodeURI(url))
    .then((response) => {
      return response.json();
    })
    .then((matches) =>
      matches
        .filter((elt) => elt.hasOwnProperty('label'))
        .map((elt) => elt.label)
    );
}
