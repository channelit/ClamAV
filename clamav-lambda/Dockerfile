FROM centos:7 AS clam-build

RUN yum install -y epel-release
RUN yum install -y clamav
RUN yum install -y clamd

FROM amazon/aws-lambda-java

RUN yum install shadow-utils -y

ENV LAMBDA_TASK_ROOT="/var/task"

RUN yum install ca-certificates
RUN update-ca-trust
RUN update-ca-trust force-enable

RUN yum install sudo -y
#
#RUN /usr/sbin/groupadd clamav
#RUN /usr/sbin/useradd clamav -g clamav

COPY --from=clam-build /usr/bin/clamscan ${LAMBDA_TASK_ROOT}
COPY --from=clam-build /usr/bin/freshclam ${LAMBDA_TASK_ROOT}
COPY --from=clam-build /usr/lib64/*.so.* ${LAMBDA_TASK_ROOT}/
COPY --from=clam-build /etc/pki/ca-trust/source/anchors/* /etc/pki/ca-trust/source/anchors/

RUN echo "DatabaseMirror database.clamav.net" > ${LAMBDA_TASK_ROOT}/freshclam.conf
RUN echo "CompressLocalDatabase yes" >> ${LAMBDA_TASK_ROOT}/freshclam.conf

COPY target/classes ${LAMBDA_TASK_ROOT}
COPY target/dependency/* ${LAMBDA_TASK_ROOT}/lib/

#RUN sudo chown -R clamav:clamav ${LAMBDA_TASK_ROOT}

#USER clamav