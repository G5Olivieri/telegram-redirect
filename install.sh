apt install cmake build-essential git gperf zlib1g-dev libssl-dev openjdk-11-jdk

git clone https://github.com/tdlib/td
git clone https://github.com/G5Olivieri/telegram-redirect

mkdir td/jnibuild
cd td/jnibuild
cmake -DCMAKE_BUILD_TYPE=Release -DTD_ENABLE_JNI=ON -DCMAKE_INSTALL_PREFIX:PATH=../example/java/td ..
cmake --build . --target install

cd ../../telegram-redirect/tdjni
cmake . && cmake --build .

cd ..
./gradlew build --no-daemon
