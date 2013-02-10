SOURCES = $(wildcard *.java)

all: LazySkipListTester

.PHONY: LazySkipListTester
LazySkipListTester: $(SOURCES)
	javac $(SOURCES)

.PHONY: run
run: LazySkipListTester
	java $<

.PHONY: update_and_run
update_and_run: git pull
	run