SOURCES = $(wildcard *.java)

all: LazySkipListTester

.PHONY: LazySkipListTester
LazySkipListTester: $(SOURCES)
	javac $(SOURCES)

.PHONY: run
run: LazySkipListTester
	java $<
