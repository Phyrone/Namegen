FROM openjdk:11-slim
COPY . /build
COPY names.txt /app/namegen/names.txt
RUN apt-get update -y
RUN apt-get install maven -y
RUN  cd /build && mvn clean install
RUN mkdir -p /app/namegen/ && cp /build/target/Namegen.jar /app/namegen/Namegen.jar
WORKDIR /app/namegen
RUN rm -R /build
CMD java -jar Namegen.jar
EXPOSE 8080
VOLUME ["/app/namegen"]
