SOURCES = $(wildcard primes13/*.java)

all: LazySkipListTester

.PHONY: LazySkipListTester
LazySkipListTester: $(SOURCES)
	javac $(SOURCES)

.PHONY: run
run: LazySkipListTester
	java -cp . primes13.LazySkipListTester