FROM mozilla/sbt:8u171_1.1.6

# Install python and moto for spinning up a mock S3 service
RUN apt-get update --fix-missing && \
  apt-get install -y \
    bc libpython-dev python python-pip sbt && \
  sbt sbtVersion
RUN pip install flask moto

# Copy the project files into the container.
COPY . /root

WORKDIR /root
