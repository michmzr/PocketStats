#!/bin/bash
# Local existing-VM fixture only. No deployed JAR, Atlas or protected env mounts.
set -euo pipefail
runtime=localhost/pocketstats-private-runtime:20261002
proxy=docker.io/library/nginx@sha256:8f84ed99befc3891b8f329c5c202785278a2cfb7c25107d57fb2a134a3117433
src=/private/tmp/dev-server-pocketstats-coolify/deploy/coolify
fixture=$(mktemp -d /var/tmp/pocketstats-runtime-fixture.XXXXXX)
network=pocketstats-runtime-fixture
cleanup() {
  podman rm -f pocketstats-fixture-proxy pocketstats-fixture-backend >/dev/null 2>&1 || true
  podman network rm "$network" >/dev/null 2>&1 || true
  rm -rf "$fixture"
}
trap cleanup EXIT
chmod 755 "$fixture"
cp "$src/pocketstats-launch" "$src/nginx.conf" "$fixture/"
printf '%s\n' '{"version":2,"database":"test","mode":"shared-dev-dashboard","source_unit":"pocketstats.service","source_state":"active-authoritative","evidence_id":"synthetic-local-runtime"}' > "$fixture/marker.json"
chown 0:982 "$fixture/marker.json"
chmod 640 "$fixture/marker.json"
common=(--pull=never --platform=linux/amd64 --user=999:982 --read-only --cap-drop=ALL --security-opt=no-new-privileges --security-opt=label=disable --memory=128m --memory-swap=128m --cpus=.5 --pids-limit=48 --tmpfs=/tmp:rw,size=8m,mode=1777)
podman run --rm "${common[@]}" --network=none --entrypoint=/bin/sh "$runtime" -c 'id; /usr/bin/java -version; /usr/bin/python3 --version; test -x /usr/bin/java; test -x /usr/bin/python3; cat /sys/fs/cgroup/memory.max /sys/fs/cgroup/memory.swap.max /sys/fs/cgroup/cpu.max /sys/fs/cgroup/pids.max; test ! -w /etc'
guard=(--env=MONGODB_URI=mongodb://synthetic.invalid/test --env=MONGODB_DB=test --env=POCKET_CONSUMER_KEY=synthetic --env=READER_ACCESS_TOKEN=synthetic --volume="$fixture/pocketstats-launch:/guard:ro" --volume="$fixture/marker.json:/etc/pocketstats-dashboard.json:ro")
podman run --rm "${common[@]}" --network=none "${guard[@]}" "$runtime" /guard --check-only
chmod 660 "$fixture/marker.json"
set +e
podman run --rm "${common[@]}" --network=none "${guard[@]}" "$runtime" /guard --check-only
code=$?
set -e
test "$code" -eq 78
chmod 640 "$fixture/marker.json"
printf 'Actual nonroot marker acceptance/rejection passed\n'
mkdir "$fixture/html"
printf 'synthetic-ui\n' > "$fixture/html/index.html"
cat > "$fixture/backend.py" <<'PY'
from http.server import BaseHTTPRequestHandler, HTTPServer
import json
class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args): pass
    def do_GET(self): self.reply()
    def do_POST(self): self.reply()
    def reply(self):
        body=json.dumps({'path':self.path,'method':self.command,'cookie':self.headers.get('Cookie'),'authorization':self.headers.get('Authorization')}).encode()
        self.send_response(200);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(body)));self.end_headers();self.wfile.write(body)
HTTPServer(('0.0.0.0',18080),Handler).serve_forever()
PY
cat > "$fixture/client.py" <<'PY'
import urllib.request, urllib.error, json, time
base='http://pocketstats-proxy:8080'
def request(path,method='GET',status=200):
    req=urllib.request.Request(base+path,method=method,headers={'Cookie':'synthetic','Authorization':'synthetic'},data=b'{}' if method=='POST' else None)
    try: response=urllib.request.urlopen(req,timeout=3)
    except urllib.error.HTTPError as error: response=error
    assert response.status==status,(path,method,response.status)
    assert response.headers['Cache-Control']=='no-store'
    assert response.headers['X-Content-Type-Options']=='nosniff'
    return response.read()
for attempt in range(30):
    try: assert request('/')==b'synthetic-ui\n';break
    except (OSError,AssertionError):
        if attempt==29: raise
        time.sleep(.2)
for path in ('langs','byPeriods','topTags','heatmap','byDay'):
    method='POST' if path=='byDay' else 'GET'
    payload=json.loads(request('/stats/'+path,method))
    assert payload=={'path':'/pocketstats/stats/'+path,'method':method,'cookie':None,'authorization':None},payload
    request('/stats/'+path,'GET' if method=='POST' else 'POST',403)
for path in ('/sync','/auth','/api/import','/stats/unknown','/.env'):
    request(path,status=403)
print('Actual Nginx fixture: five routes, five forbidden methods, five denied paths, stripped credentials, private static UI passed')
PY
chmod 644 "$fixture/backend.py" "$fixture/client.py" "$fixture/nginx.conf"
podman network create --internal "$network" >/dev/null
podman run -d --name=pocketstats-fixture-backend "${common[@]}" --network="$network" --network-alias=pocketstats --volume="$fixture/backend.py:/backend.py:ro" "$runtime" /backend.py >/dev/null
nginxargs=(--pull=never --platform=linux/amd64 --user=101:101 --read-only --cap-drop=ALL --security-opt=no-new-privileges --security-opt=label=disable --memory=64m --memory-swap=64m --cpus=.1 --pids-limit=48 --tmpfs=/tmp:rw,size=8m,mode=1777 --network="$network" --entrypoint=nginx --volume="$fixture/nginx.conf:/etc/nginx/nginx.conf:ro" --volume="$fixture/html:/usr/share/nginx/html:ro")
podman run --rm "${nginxargs[@]}" "$proxy" -t
podman run -d --name=pocketstats-fixture-proxy --network-alias=pocketstats-proxy "${nginxargs[@]}" "$proxy" -g 'daemon off;' >/dev/null
podman run --rm "${common[@]}" --network="$network" --volume="$fixture/client.py:/client.py:ro" "$runtime" /client.py
podman exec pocketstats-fixture-proxy sh -c 'id; cat /sys/fs/cgroup/memory.max /sys/fs/cgroup/memory.swap.max /sys/fs/cgroup/cpu.max /sys/fs/cgroup/pids.max; test ! -w /etc/nginx; test -w /tmp'
podman port pocketstats-fixture-proxy
printf 'No fixture ports published; cleanup follows\n'
