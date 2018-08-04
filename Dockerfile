FROM openjdk:10
COPY . /build
RUN apt-get update -y
RUN apt-get install maven -y
RUN  cd /build && mvn clean install
RUN cp /build/target/Namegen.jar /app/namegen/Namegen.jar
WORKDIR /app/namegen
CMD java -jar Namegen.jar
EXPOSE 8080
VOLUME ["/app/namegen"]