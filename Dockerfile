FROM openjdk:14-jdk-slim-buster

COPY . /app

RUN apt-get update \
  && apt-get install -y gcc build-essential git cmake zlib1g zlib1g-dev libssl-dev openssl gperf \
  && git clone http://github.com/tdlib/td \
  && mkdir -p td/jnibuild \
  && cd td/jnibuild \
  && cmake -DCMAKE_BUILD_TYPE=Release -DTD_ENABLE_JNI=ON .. \
  && cmake --build . -j 4 --target install

RUN cd /app/tdjni \
  && cmake . \
  && cmake --build .

RUN cd /app \
  && ./gradlew build --no-daemon

WORKDIR /app

ENTRYPOINT ["java", "-Djava.library.path=tdjni", "-jar", "build/libs/app.jar"]
