FROM openjdk:8
ENV mailspider /home/mailspider
RUN mkdir ${mailspider}
WORKDIR ${mailspider}
#install maven
RUN apt-get install wget
RUN cd /opt && \
    wget http://mirror.linux-ia64.org/apache/maven/maven-3/3.5.3/binaries/apache-maven-3.5.3-bin.zip && \
    unzip apache-maven-3.5.3-bin.zip && \
    cd apache-maven-3.5.3 && ls -la
ENV PATH="/opt/apache-maven-3.5.3/bin"
ENV M3_HOME=/opt/apache-maven-3.5.3

RUN mvn -v

#build and run mailspider
#RUN cd ${mailspider} && mvn build
#COPY . .
