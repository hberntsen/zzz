TEXFILE=doc

SRCDIR=.
TMPDIR=${SRCDIR}/tmp

TEXFLAGS=-output-directory ${TMPDIR}

build: ${SRCDIR}/*.tex images.tex
	mkdir -p ${TMPDIR}
	pdflatex ${TEXFLAGS} ${SRCDIR}/${TEXFILE}.tex
	mv -f ${TMPDIR}/${TEXFILE}.pdf ${SRCDIR}/

images.tex: graphs
	find data -name '*.pdf' | sort | awk '{print "\\includegraphics[width=\\linewidth]{./" $$1 "}"}' > images.tex

graphs: $(patsubst %,%.pdf,$(wildcard data/*_accelerometer))

data/%.pdf: data/% graph.py
	python3 ./graph.py $<

clean:
	rm -rf ${TMPDIR}
	rm data/*.pdf

distclean: clean
	rm ${SRCDIR}/${TEXFILE}.pdf


