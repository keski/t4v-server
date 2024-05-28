mvn clean package

docker run \
    --rm \
    -it \
    -v ./target/t4v-server.jar:/t4v-server.jar \
    -p 8080:8080 \
    openjdk:latest \
    /bin/bash -c '/usr/bin/java -jar /t4v-server.jar'
