FROM docker.io/nginxinc/nginx-unprivileged@sha256:209331cfcaec00da781f5b8a38e0d1c0abd00cb2b51e6ad385a30abbbdb04e15
COPY src/frontend/dist/ /usr/share/nginx/html/
COPY deploy/coolify/nginx.conf /etc/nginx/nginx.conf
COPY deploy/coolify/dashboard-start /usr/local/libexec/dashboard-start
USER 101:101
ENTRYPOINT ["/bin/sh", "/usr/local/libexec/dashboard-start"]
