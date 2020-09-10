build:
	docker-compose run --rm app bash -c "./gradlew build --no-daemon"

run:
	docker-compose run --rm app bash -c "java '-Djava.library.path=tdjni' -jar build/libs/app.jar"

shell:
	docker-compose run --rm app bash

generate_api:
	docker-compose run --rm bash -c "td_generate_java_api TdApi /usr/local/bin/td/generate/scheme/td_api.tlo src/main/java/ org/glayson/telegram"

compile_lib:
	docker-compose run --rm bash -c "cd tdjni; cmake && cmake --build ."
