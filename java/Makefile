SOURCES = $(wildcard *.java)

all: LazySkipListTester

.PHONY: LazySkipListTester
LazySkipListTester: $(SOURCES)
	javac $(SOURCES)

.PHONY: run
run: LazySkipListTester
	java $<

.PHONY: update_and_run
run: git pull
	run