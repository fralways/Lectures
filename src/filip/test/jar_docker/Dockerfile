FROM anapsix/alpine-java
MAINTAINER filip.raj@hotmail.com

COPY Lectures.jar Lectures.jar
COPY script.sh script.sh
RUN chmod +x script.sh
EXPOSE 8000 8210

CMD ["bash", "script.sh"]