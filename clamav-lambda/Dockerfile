FROM centos:8 AS clam-build

RUN dnf --enablerepo=extras install epel-release -y
RUN dnf update -y
RUN dnf install clamav clamav-update clamd -y

FROM amazon/aws-lambda-java

RUN yum install shadow-utils -y
RUN yum install https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm -y
RUN yum install clamav clamav-update clamd -y
ENV LAMBDA_TASK_ROOT="/var/task"

RUN yum install ca-certificates
RUN update-ca-trust
RUN update-ca-trust force-enable

RUN yum install sudo -y

RUN /usr/sbin/groupadd clamav
RUN /usr/sbin/useradd clamav -g clamav

RUN cp /usr/bin/clamscan ${LAMBDA_TASK_ROOT}
RUN cp /usr/bin/freshclam ${LAMBDA_TASK_ROOT}
RUN cp /usr/lib64/*.so.* ${LAMBDA_TASK_ROOT}/
#RUN cp /etc/pki/ca-trust/source/anchors/* /etc/pki/ca-trust/source/anchors/

RUN sudo echo "DatabaseMirror database.clamav.net" > ${LAMBDA_TASK_ROOT}/freshclam.conf
RUN sudo echo "CompressLocalDatabase yes" >> ${LAMBDA_TASK_ROOT}/freshclam.conf

COPY target/classes ${LAMBDA_TASK_ROOT}
COPY target/dependency/* ${LAMBDA_TASK_ROOT}/lib/

#RUN yum install wget sudo tar -y
#RUN wget https://dl.google.com/go/go1.13.4.linux-amd64.tar.gz
#RUN sudo tar -C /usr/local -xf go1.13.4.linux-amd64.tar.gz
#RUN export PATH=$PATH:/usr/local/go/bin

RUN sudo chown -R clamav:clamav ${LAMBDA_TASK_ROOT}
#
USER clamav