FROM openjdk:8

ENV SCALA_VERSION=2.12.4 \
    SBT_VERSION=1.0.4

# Install Scala.
RUN \
  curl -fsL https://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /root/ && \
  echo >> /root/.bashrc && \
  echo "export PATH=~/scala-$SCALA_VERSION/bin:$PATH" >> /root/.bashrc

# Install sbt.
RUN \
  curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb

RUN apt-get update --fix-missing && \
  apt-get install -y \
    bc libpython-dev python python-pip sbt && \
  sbt sbtVersion

# The Python lib moto is for a mock S3 service.
RUN pip install flask moto

# Copy the project files into the container.
COPY . /root

WORKDIR /root
