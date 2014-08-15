# Based on trusted builds.
FROM debian:stable

# Upgrade to the latest packages.
RUN apt-get update
RUN apt-get upgrade -y

# Install build chain tools.
RUN apt-get install -y git wget

# Install Python.
RUN apt-get install -y python python-dev python-pip python-virtualenv

# Install Redis.
RUN \
  cd /tmp && \
  wget http://download.redis.io/redis-stable.tar.gz && \
  tar xvzf redis-stable.tar.gz && \
  cd redis-stable && \
  make && \
  make install && \
  cp -f src/redis-sentinel /usr/local/bin && \
  mkdir -p /etc/redis && \
  cp -f *.conf /etc/redis && \
  rm -rf /tmp/redis-stable* && \
  sed -i 's/^\(bind .*\)$/# \1/' /etc/redis/redis.conf && \
  sed -i 's/^\(daemonize .*\)$/# \1/' /etc/redis/redis.conf && \
  sed -i 's/^\(dir .*\)$/# \1\ndir \/data/' /etc/redis/redis.conf && \
  sed -i 's/^\(logfile .*\)$/# \1/' /etc/redis/redis.conf

# Install Node.js.
RUN \
  cd /tmp && \
  wget http://nodejs.org/dist/node-latest.tar.gz && \
  tar xvzf node-latest.tar.gz && \
  rm -f node-latest.tar.gz && \
  cd node-v* && \
  ./configure && \
  CXX="g++ -Wno-unused-local-typedefs" make && \
  CXX="g++ -Wno-unused-local-typedefs" make install && \
  cd /tmp && \
  rm -rf /tmp/node-v* && \
  echo '\n# Node.js\nexport PATH="node_modules/.bin:$PATH"' >> /root/.bash_profile

# Define mountable directories.
VOLUME ["/pssst"]

# Define working directory.
WORKDIR /pssst

# Install Pssst.
RUN \
  cd /tmp && \
  git clone https://github.com/pssst/pssst/ latest && \
  mv latest/server/* /pssst && \
  rm -rf latest && \
  cd /pssst && \
  npm install

# Expose ports.
EXPOSE 80

# Define default command.
CMD ["redis-server", "/etc/redis/redis.conf", "&", "node", "start"]
