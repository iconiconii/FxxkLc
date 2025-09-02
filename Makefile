.PHONY: dev backend-dev backend-build backend-test frontend-dev frontend-build frontend-start frontend-lint e2e build docker-up docker-down docker-logs

dev:
	@npm run dev

backend-dev:
	mvn spring-boot:run -Dspring-boot.run.profiles=dev

backend-build:
	mvn -q -DskipTests package

backend-test:
	mvn test

frontend-dev:
	npm --prefix frontend run dev

frontend-build:
	npm --prefix frontend run build

frontend-start:
	npm --prefix frontend run start

frontend-lint:
	npm --prefix frontend run lint

e2e:
	npm --prefix frontend run test:e2e

build: backend-build frontend-build

docker-up:
	docker-compose up -d

docker-down:
	docker-compose down

docker-logs:
	docker-compose logs -f --tail=100

