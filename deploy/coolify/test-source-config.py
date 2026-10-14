#!/usr/bin/env python3
"""Offline admission for the source-built private Compose profile."""
import json
from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parent

class SourceConfigurationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.config = json.loads(subprocess.check_output([
            'ruby', '--disable-gems', '-ryaml', '-rjson', '-e',
            'puts JSON.generate(YAML.safe_load(File.read(ARGV[0]), aliases: true))',
            str(ROOT / 'compose-source.yaml')], text=True))
        cls.services = cls.config['services']
        cls.proxy = (ROOT / 'nginx-source.conf').read_text()

    def test_source_images_own_code_and_configuration(self):
        mounts = {name: {m['target'] for m in service.get('volumes', [])}
                  for name, service in self.services.items()}
        self.assertEqual(mounts['pocketstats'],
            {'/etc/pocketstats-dashboard.json', '/var/lib/pocketstats', '/home/app'})
        self.assertEqual(mounts['dashboard'], set())
        for service in self.services.values():
            self.assertEqual(service['pull_policy'], 'always')
            self.assertNotIn('build', service)
            self.assertNotIn('env_file', service)
        self.assertIn('COPY build/libs/app.jar', (ROOT / 'APP.Dockerfile').read_text())
        self.assertIn('COPY src/frontend/dist/', (ROOT / 'DASHBOARD.Dockerfile').read_text())

    def test_native_environment_and_private_boundaries(self):
        backend = self.services['pocketstats']; dashboard = self.services['dashboard']
        self.assertEqual(set(backend['environment']),
            {'MONGODB_URI', 'MONGODB_DB', 'POCKET_CONSUMER_KEY', 'READER_ACCESS_TOKEN'})
        self.assertEqual(dashboard['environment'], {'VUE_APP_BACKEND_URL': '${VUE_APP_BACKEND_URL:-}'})
        self.assertNotIn('ports', backend)
        self.assertEqual(dashboard['ports'], ['127.0.0.1:18081:8080'])
        self.assertEqual(backend['networks'], ['dashboard', 'atlas_egress'])
        self.assertEqual(dashboard['networks'], ['dashboard', 'proxy_ingress'])
        self.assertEqual(self.config['networks']['dashboard'], {'internal': True})
        self.assertEqual(self.config['networks']['proxy_ingress']['driver_opts'],
            {'com.docker.network.bridge.name': 'ps-ui-ingress'})

    def test_nonroot_resources_marker_and_read_only_filesystems(self):
        for name, user, ram, pids in (('pocketstats', '999:982', '768m', 128),
                                      ('dashboard', '101:101', '64m', 48)):
            service = self.services[name]
            self.assertEqual(service['user'], user)
            self.assertEqual(service['mem_limit'], ram)
            self.assertEqual(service['memswap_limit'], ram)
            self.assertEqual(service['pids_limit'], pids)
            self.assertTrue(service['read_only'])
            self.assertEqual(service['cap_drop'], ['ALL'])
            self.assertEqual(service['security_opt'], ['no-new-privileges:true'])
            self.assertIn('healthcheck', service)
        marker = self.services['pocketstats']['volumes'][0]
        self.assertEqual(marker['target'], '/etc/pocketstats-dashboard.json')
        self.assertTrue(marker['read_only'])
        self.assertFalse(marker['bind']['create_host_path'])

    def test_new_library_reads_and_sync_denial(self):
        self.assertEqual(self.proxy.count('proxy_pass '), 9)
        for name in ('langs', 'byPeriods', 'topTags', 'heatmap', 'byDay', 'library'):
            start = self.proxy.index('location = /stats/' + name + ' {')
            end = self.proxy.find('location ', start + 1)
            block = self.proxy[start:end if end >= 0 else len(self.proxy)]
            self.assertIn('limit_except ' + ('POST' if name == 'byDay' else 'GET') + ' { deny all; }', block)
            self.assertIn('proxy_set_header Authorization "";', block)
            self.assertIn('proxy_set_header Cookie "";', block)
        availability = self.proxy.split('location = /sync/availability {')[1].split('}')[0]
        self.assertIn('limit_except GET', availability)
        self.assertIn('proxy_pass http://pocketstats:18080/pocketstats/sync/availability;', self.proxy)
        self.assertIn('limit_except GET POST', self.proxy)
        self.assertIn('if ($reject_sync_write) { return 403; }', self.proxy)
        self.assertIn('\"POST:1\" 0;', self.proxy)
        self.assertRegex(self.proxy, r'location /\s*\{\s*return 403;\s*\}')

if __name__ == '__main__':
    unittest.main(verbosity=2)
