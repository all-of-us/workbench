from __future__ import print_function
from flask import Flask,render_template,request,url_for, jsonify
from flask_restful import Resource,Api,reqparse
from flask_cors import CORS,cross_origin
import pandas as pd
import sys
import json
import numpy as np
import os
import warnings
warnings.simplefilter(action='ignore', category=FutureWarning)


app=Flask(__name__)
api=Api(app)

CORS(app,resources={r"/api/*": {"origins": "*"}})


PheRS_map=pd.read_table('/Users/srushti/PheRS/PheRS_Python/disease_to_phecodes.txt', sep="\t")
icd9_to_phecodes=pd.read_table("/Users/srushti/PheRS/PheRS_Python/icd9_to_phecode.txt", sep="\t",dtype = {'icd9':np.object,'phecode':np.object})


diseases=PheRS_map.MIM.tolist()
icd9_codes=icd9_to_phecodes.icd9.tolist()

icd9s=pd.DataFrame(columns=["ID","icd9"])

icd9s_from_file = pd.DataFrame()
phes_from_file = pd.DataFrame()

diseaseCode=str()

PheRS_Score=0

diseaseMatchingPhecodes=[]

parser = reqparse.RequestParser()
parser.add_argument('codes', type=list)
parser.add_argument('diseaseCode')

class DiseaseCodes(Resource):
        def get(self):
                return {'disease_codes':diseases}

class Icd9Codes(Resource):
	def get(self):
		c=[]
		for code in icd9_codes:
			c.append({"name":code,"value":code})
		return {'icd9_codes':c}

@app.after_request
def after_request(response):
  response.headers.add('Access-Control-Allow-Origin', '*')
  response.headers.add('Access-Control-Allow-Headers', 'Content-Type,Authorization')
  response.headers.add('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE,OPTIONS')
  return response

@app.route('/uploadFile', methods=['POST', 'OPTIONS'])
def uploadFile():
	icd9_codes=[]
	icd9s=pd.DataFrame()
	global icd9s_from_file
	if request.method == 'POST':
		try:
			file=request.files['file[]']
			if file:
				#fileDir='/app001/www/html/PheRS/Python_Code/FlaskApp/'+file.filename
				#file.save(file.filename)
				file_content=file.read()
				#print(file_content)
				file_content.replace("\r\n","\n")
				file_lines=file_content.split("\n")
				codes=[]
				for i in range(1,len(file_lines)):
					line=file_lines[i].decode()
					line=line.replace('\r','')
					id_code=line.split(",")
					if len(id_code) == 2:
						codes.append(id_code[1].encode("utf-8"))
					
				
				ids=['1']*len(codes)
				icd9s=pd.DataFrame({'ID':ids,'icd9':codes})
				icd9s_from_file=icd9s
		except Exception as ex:
			print(ex)
			pass
	return "file uploaded succesfully!!"


@app.route('/uploadPheFile', methods=['POST', 'OPTIONS'])
def uploadPheFile():
	phecodes = []
	phes = pd.DataFrame()
	global phes_from_file
	if request.method == 'POST':
		try:
			file = request.files['file[]']
			if file:
				file_content = file.read()
				file_content.replace("\r\n", "\n")
				file_lines = file_content.split("\n")
				codes = []
				for i in range(1, len(file_lines)):
					line = file_lines[i].decode()
					line = line.replace('\r', '')
					id_code = line.split(",")
					if len(id_code) == 2:
						codes.append(id_code[1].encode("utf-8"))

				ids = ['1'] * len(codes)
				phes = pd.DataFrame({'ID': ids, 'phecode': codes})
				phes_from_file = phes
		except Exception as ex:
			print(ex)
			pass
	return "file uploaded succesfully!!"



class Score(Resource):
	def get(self):
		#print(icd9s_from_file)
		global diseases
		global icd9s_from_file
		icd9s=pd.DataFrame()
		sArgs=parser.parse_args()
		codes=sArgs['codes']
		codeString=''.join(map(str,codes))
		codes=codeString.split(",")
		diseaseCode=sArgs['diseaseCode']
		if diseaseCode not in diseases:
			return {'score':0,'diseaseMatchingPhecodes':None}
		PheRS_Score=0
		score=0

		if len(codes)==0:
			icd9s=icd9s_from_file
		else:
			ids=[1]*len(codes)
			icd9s=pd.DataFrame({'ID':ids,'icd9':codes})

		diseaseMatchingPhecodes=[]
		phecodes=pd.merge(icd9s,icd9_to_phecodes,on="icd9")

		phecodes=phecodes[['ID','phecode']].copy() 
		phecodes=phecodes.drop_duplicates('phecode')
		phecodes["value"]=1
		phecodes=phecodes.pivot(index='ID',columns='phecode',values='value')

		weights=pd.read_table('/Users/srushti/PheRS/PheRS_Python/weights_VUMC_discovery.txt', sep="\t",dtype={'phecode':np.object,'case_count':np.int64,'prev':np.float64,'w':np.float64})
		weights=weights.rename(index=weights['phecode'])

		phes=phecodes.columns.tolist()


		for i in range(len(phes)):
			phe=phes[i]
			try:
				phecodes[[phe]]=phecodes[[phe]]*weights[weights.phecode==phe].w[0]
			except IndexError:
				phecodes[[phe]]=phecodes[[phe]]*0

		phecode_list=PheRS_map[PheRS_map.MIM==diseaseCode].phecodes.item()
		phecode_list=phecode_list.split(',')

		#print(phecode_list)
		#print(phes)

		for phecode in phecode_list:
			if phecode in phes:
				score=score+phecodes[phecode].item()
				diseaseMatchingPhecodes.append(phecode)
			else:
				pass
		PheRS_Score=str(round(score,2))
		print(diseaseMatchingPhecodes)
		print(PheRS_Score)
		return {'score':PheRS_Score,'diseaseMatchingPhecodes':diseaseMatchingPhecodes}

class ScoreWithPhe(Resource):
	def get(self):
		sArgs=parser.parse_args()
		codes = sArgs['codes']
		codeString = ''.join(map(str, codes))
		codes = codeString.split(",")
		diseaseCode=sArgs['diseaseCode']
		if diseaseCode not in diseases:
			return {'score':0,'diseaseMatchingPhecodes':None}
		PheRS_Score=0
		score=0
		phes = []

		if len(codes)==0:
			return {'score': 0, 'diseaseMatchingPhecodes': None}
		else:
			phes=codes

		phecodes = pd.DataFrame()
		phecodes['phecode']=phes
		phecodes['value']=1
		phecodes['ID']=1

		phecodes = phecodes.pivot(index='ID', columns='phecode', values='value')

		diseaseMatchingPhecodes=[]

		weights=pd.read_table('/Users/srushti/PheRS/PheRS_Python/weights_VUMC_discovery.txt', sep="\t",dtype={'phecode':np.object,'case_count':np.int64,'prev':np.float64,'w':np.float64})
		weights=weights.rename(index=weights['phecode'])

		for i in range(len(phes)):
			phe=phes[i]
			try:
				phecodes[[phe]]=phecodes[[phe]]*weights[weights.phecode==phe].w[0]
			except IndexError:
				phecodes[[phe]]=phecodes[[phe]]*0

		phecode_list=PheRS_map[PheRS_map.MIM==diseaseCode].phecodes.item()
		phecode_list=phecode_list.split(',')

		#print(phecode_list)

		for phecode in phecode_list:
			if phecode in phes:
				score=score+phecodes[phecode].item()
				diseaseMatchingPhecodes.append(phecode)
			else:
				pass
		PheRS_Score=str(round(score,2))
		print(PheRS_Score)
		print(diseaseMatchingPhecodes)

		return {'score':PheRS_Score,'diseaseMatchingPhecodes':diseaseMatchingPhecodes}

		
@app.route("/")
def index():
	return "Python code to calculate score"

@app.route('/autocomplete_icd9Codes',methods=['GET'])
def autocomplete_icd9Codes():
	icd9Codes=icd9_codes
	c=[]
	for code in icd9_codes:
		c.append({"name":code,"value":code})	
	return jsonify(codes=c)

@app.route("/index")
def home():
	return "Python code to calculate score"



api.add_resource(DiseaseCodes,'/diseaseCodes')
api.add_resource(Icd9Codes,'/icd9Codes')
api.add_resource(Score,'/score')
api.add_resource(ScoreWithPhe,'/scoreWithPhecodes')
#api.add_resource(UploadFile,'/uploadFile')
api.add_resource(UploadPheFile,'/uploadPheFile')



if __name__=='__main__':
	app.run(debug=True)

	
















