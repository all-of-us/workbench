FROM node:8

ADD . /app
WORKDIR /app

# shared-git-hooks needs this to be a git dir
RUN git init

RUN npm install

CMD ["npm", "run-script", "ng",  "--", "serve", "--host", "0.0.0.0"]
