# Download base image ubuntu 14.04
# Based on goyalzz/ubuntu-java-8-maven-docker-image
FROM ubuntu:trusty

MAINTAINER Shamil Gumirov <shamil.gumirov@gmail.com>

WORKDIR /home/mailspider

# MailSpider components versions:
ENV MAILSPIDER_BASE_VER 1.9

# Prepare installation of Oracle Java 8
ENV JAVA_VER 8
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

# Set locales
RUN locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

#---------------------------------
# Install requirements: git, wget, Oracle Java8
#---------------------------------

RUN echo 'deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main' >> /etc/apt/sources.list && \
    echo 'deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main' >> /etc/apt/sources.list && \
    echo 'deb http://archive.ubuntu.com/ubuntu trusty main universe' >> /etc/apt/sources.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C2518248EEA14886 && \
    apt-get update && \
    apt-get install -y git wget && \
    echo oracle-java${JAVA_VER}-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    apt-get install -y --force-yes --no-install-recommends oracle-java${JAVA_VER}-installer oracle-java${JAVA_VER}-set-default && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
    rm -rf /var/cache/oracle-jdk${JAVA_VER}-installer

# Set Oracle Java as the default Java
RUN update-java-alternatives -s java-8-oracle
RUN echo "export JAVA_HOME=/usr/lib/jvm/java-8-oracle" >> ~/.bashrc

# Install maven 3.5.3
RUN wget --no-verbose -O /tmp/apache-maven-3.5.3-bin.tar.gz http://mirror.linux-ia64.org/apache/maven/maven-3/3.5.3/binaries/apache-maven-3.5.3-bin.tar.gz && \
    tar xzf /tmp/apache-maven-3.5.3-bin.tar.gz -C /opt/ && \
    ln -s /opt/apache-maven-3.5.3 /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/local/bin  && \
    rm -f /tmp/apache-maven-3.5.3-bin.tar.gz

ENV MAVEN_HOME /opt/maven

#--------------------
# Install mailspider
#--------------------

ADD ./spiderplugins /home/mailspider/spiderplugins
COPY . /home/mailspider/mailspider

# Install mailspider-base and spiderplugins from our remote repo

RUN cd /home/mailspider && git clone -b $MAILSPIDER_BASE_VER https://github.com/sgumirov/mailspider-base.git
RUN cd /home/mailspider/mailspider-base && mvn clean test install

# Clone private repo spiderPlugins
# add credentials on build
#ARG SSH_PRIVATE_KEY
#RUN mkdir /root/.ssh/
#RUN echo "${SSH_PRIVATE_KEY}" > /root/.ssh/id_rsa

# make sure your domain is accepted
#RUN touch /root/.ssh/known_hosts
#RUN ssh-keyscan bitbucket.org >> /root/.ssh/known_hosts

#RUN cd /home/mailspider && git clone https://bitbucket.com/shamilg1/spiderPlugins.git
#RUN cd /home/mailspider && git clone https://shamilg1@bitbucket.org/partsibteam/spiderplugins.git
RUN cd /home/mailspider/spiderplugins && mvn clean test install

#--------------
# Test image
#--------------

RUN cd /home/mailspider/mailspider && mvn clean test build
#mvn clean build install
