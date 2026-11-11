.PHONY: setup build test lint run clean

setup:
	./scripts/setup.sh

build:
	./scripts/build.sh

test:
	./scripts/test.sh

lint:
	./scripts/lint.sh

run:
	./scripts/run.sh

clean:
	./scripts/clean.sh
