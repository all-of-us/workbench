clean-pyc:
	find . | grep -E "(__pycache__|\.pyc|\.pyo)" | xargs rm -rf

clean-dist:
	rm -rf dist/ build/ .eggs/ *.egg *.egg-info

clean: clean-pyc clean-dist

build:
	python setup.py sdist
	python setup.py bdist_wheel

install: clean
	python setup.py install

release: clean build
	# Release requires twine & appropriate credentials: `pip install twine`
	twine upload --config-file=./.pypirc dist/*

test:
	python setup.py test
