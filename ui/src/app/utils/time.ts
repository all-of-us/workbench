import * as moment from "moment";

export const ago = (timeEpoch: number) => {
  return moment(timeEpoch).fromNow();
}

export const verboseDatetime = (timeEpoch: number) => {
  return moment(timeEpoch).format('MMMM Do YYYY, h:mm:ss a');
}
