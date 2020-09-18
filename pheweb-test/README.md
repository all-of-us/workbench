# Pheweb Integration

## What will Pheweb do?

Pheweb shows correlations between genotypes and phenotypes by looking at gene variants

e.g. What is the correclation between the Braca-1 gene and Breast Cancer?

Pheweb also provides the abilito to search by gene, mutation and phenotype to see what correlations are calculated

Pheweb is strictly a viewer/visulizer, it does not perform any analysis.

## Dependency list

These are some of the dependency issues I ran into while trying different things out with PheWeb:

1. pysam
2. cython
3. python environment
4. libbz2-dev or bzip2-devel
5. pyliblzma
6. liblzma-dev

All of these dependencies seem to be available on app engine standard.

### Google Cloud Dependencies for Local Development

1. google-cloud-sdk-app-engine-python
2. cloud-datastore-emultator

It should be noted that PheWeb is finicky about the Python version in use. I have successfully run PheWeb with Python 3.6 and 3.8

## Does it need a live web server?

1. Pheweb can run off of gunicorn
2. Requires Python 3.8 (or 3.6)

If you want to run pheweb with gunicorn you must first run `PheWeb wsgi`

This will startup a [Flask](https://flask.palletsprojects.com/) server. The server will serve up static HTML files ([example](https://github.com/statgen/pheweb/blob/master/pheweb/serve/templates/gene.html)) that manually pull in all required javascript files using `script` tags.

The server needs to have the `generated-by-pheweb` directory available (by running `pheweb process`).
 The server will then allow users to query for a variant, gene or phenotype and route to the appropriate path based on the user query.

## Architecture of the application

1. Flask is used for the server
2. The client side does not seem to use any javascript frameworks or build tools - it consists of HTML files directly pulling in Javascript using `script` tags
3. The two visualization libraries being used are [LocusZoom](http://locuszoom.org/) and [D3](https://d3js.org/)
4. There are various routes/endpoints that will handle things like autocompletion, or serving up the HTML pages. They can be found [here](https://github.com/statgen/pheweb/blob/master/pheweb/serve/server.py)

### Drawback
It will be difficult to customize how pheweb runs in its current state. Changes to the code may be necessary

## Can it host static content?

While I do not believe PheWeb itself is setup to host static content, Appengine can be configured to host static content.

Pheweb is [not designed for static content](https://github.com/statgen/pheweb/issues/132)

## What does rendering look like in Pheweb?

Pheweb renders its visualizations on the client using Javascript and the D3 library. The code can be found in the [pheweb/serve/static](https://github.com/statgen/pheweb/tree/master/pheweb/serve/static) directory. 

## How are sessions handled?

## Deployment

### What Does The Deployed Code Look Like?

The PheWeb requirement will be added to app engine. Prior to deploying some PheWeb processes can be run locally - or in an automated infrastructure like CircleCI.

Pheweb Generates files and places them in a directory called "generated-by-pheweb" - this is where the app will get its information.

If we want to make modifications and fork PheWeb it may complicate the deployment as we may need to manually install PheWeb as part of the deployment. If we modify the open source code and work with the maintainers we would need to release a new version and app engine should be able to pick it up.

### How Is The App Deployed?

To deploy do the following:

1. Create a `pheno-list.json` file to list out all of your association files
There are utilities that can scan directories and create the pheno-list.json file for you

`pheweb phenolist glob "*.epacts.gz"`
`pheweb phenolist extract-phenocode-from-filepath --simple`

We do need to take care here - these commands did not work from an "out of the box" tutorial. I am not sure whether Pheweb conforms to semver standards.

2. Run `pheweb process`
2. Run `pheweb wsgi`
2. The yaml file should utilize gunicorn to run off of the wsgi file that gets created in the `generate-by-pheweb` directory
2. Make sure you have a `requirements.txt` file with PheWeb as a requirement
2. Deploy to app engine

The above process does the complete build locally and deploys to app engine. It is conceiveable we could create a flow where association files are uploaded and processed in the app itself

There are a few other caveats:
1. A config.py file will need to be created to setup the cache location or disable the cache.
   a. Pheweb defines the cache as "a directory where files common to all datasets can be stored"
      But I am unsure of exactly how the cache is used at this time.
2. If you do keep the cache, you will need to adjust the path location of the cache when you:
   a. Run the process locally
   b. Before you deploy to app engine so the app can find the cache when it gets uploaded

Some files can be quite large and may take some time to deploy.
I think we will need to work with some real data sets to determine the horizontal scalability and whether we can modify Pheweb to better work with our data.

## SSL

When deployed the app server is able to successfully use SSL.

### Scalability

It looks like Pheweb can scale on App Engine, but a bit more research into this may be needed. The files Pheweb creates and references can be large.

## What happens when there is a new CDR release?

We may need to rebuild the association files and rerun `pheweb process`

# Integration

## What inputs do we need to run
Association files are needed in the pheweb csv/tsv formats

## How do we get the files over to Pheweb

The pheweb files are be generated and deployed to the server.
This may not be an ideal approach for large files. But without data it is difficult to tell.


## Where Will The Files Come From

* Data Science team will generate the files/data

## Other Considerations
1. Pheweb will need to support hg38, which it currently does not.
2. Who will determine if the data is suitable for public consumption?
