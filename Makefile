JAVA_RELEASE ?= 17
BUILD_DIR := build/classes
SOURCES := $(wildcard src/*.java)

.PHONY: build run-greedy run-comparison clean

build:
	mkdir -p $(BUILD_DIR)
	javac --release $(JAVA_RELEASE) -encoding UTF-8 -d $(BUILD_DIR) $(SOURCES)

run-greedy: build
	java -cp $(BUILD_DIR) GreedyAlgorithmMain

run-comparison: build
	java -cp $(BUILD_DIR) MethodComparison

clean:
	rm -rf build
