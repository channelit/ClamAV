FROM amazon/aws-lambda-java

ARG LAMBDA_TASK_ROOT="/var/task"

RUN yum install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
RUN yum install -y epel-release
RUN yum update -y
RUN yum install -y clamav
RUN yum install -y ca-certificates

RUN mkdir -p /opt/app/bin
RUN cp /usr/bin/clamscan /usr/bin/freshclam /usr/lib64/*clam* ${LAMBDA_TASK_ROOT}

RUN echo "DatabaseMirror database.clamav.net" > ${LAMBDA_TASK_ROOT}/freshclam.conf
RUN echo "CompressLocalDatabase yes" >> ${LAMBDA_TASK_ROOT}/freshclam.conf

COPY target/classes ${LAMBDA_TASK_ROOT}
COPY target/dependency/* ${LAMBDA_TASK_ROOT}/lib/