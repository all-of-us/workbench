
export function handleErrors(response) {
  if (!response.ok) {
    throw Error(response.statusText);
  }
  return response;
}

export function fullUrl(url: string): string {
  return environment.allOfUsApiUrl + url;
}