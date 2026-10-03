.PHONY: help build run test clean

help:
	@echo "make build  - Compilar"
	@echo "make run    - Ejecutar"
	@echo "make test   - Tests"
	@echo "make clean  - Limpiar"

build:
	mvn clean package -DskipTests

run:
	mvn javafx:run

test:
	mvn clean test

clean:
	mvn clean
