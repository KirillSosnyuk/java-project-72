FROM gradle:latest

WORKDIR /app

COPY / .

RUN gradle installDist

CMD ./build/install/app/bin/app