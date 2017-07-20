TEXFILE=doc

SRCDIR=.
TMPDIR=${SRCDIR}/tmp

TEXFLAGS=-output-directory ${TMPDIR}

html: graphs_png
	cp header.html index.html
	find data -name '*.png' | sort | awk '{print "<img alt=\"\" src=\"" $$1 "\"><br>"}' >> index.html
	cat footer.html >> index.html

tmp/images.tex: graphs_pdf
	mkdir -p ${TMPDIR}
	find data -name '*.pdf' | sort | awk '{print "\\includegraphics[width=\\linewidth]{./" $$1 "}"}' > tmp/images.tex

pdf: ${SRCDIR}/*.tex tmp/images.tex
	mkdir -p ${TMPDIR}
	pdflatex ${TEXFLAGS} ${SRCDIR}/${TEXFILE}.tex
	mv -f ${TMPDIR}/${TEXFILE}.pdf ${SRCDIR}/

graphs_pdf: $(patsubst %,%.pdf,$(wildcard data/*_accelerometer))
graphs_png: $(patsubst %,%.png,$(wildcard data/*_accelerometer))

data/%.pdf: data/% graph.py
	python3 ./graph.py $< pdf

data/%.png: data/% graph.py
	python3 ./graph.py $< png

clean:
	rm -rf ${TMPDIR}
	rm data/*.pdf
	rm data/*.png
	rm index.html

distclean: clean
	rm ${SRCDIR}/${TEXFILE}.pdf

