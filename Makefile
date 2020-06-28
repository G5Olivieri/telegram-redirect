build:
	docker-compose run --rm app bash -c "./gradlew build --no-daemon"

run:
	docker-compose run --rm app bash -c "java '-Djava.library.path=tdjni' -jar build/libs/app.jar"

shell:
	docker-compose run --rm app bash
