FROM node

WORKDIR /ui

COPY package.json package-lock.json /ui/
RUN npm install

CMD ["yarn", "start"]