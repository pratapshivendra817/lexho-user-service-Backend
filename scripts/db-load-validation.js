import http from 'k6/http';
import { check, sleep } from 'k6';

const healthResponse = http.expectedStatuses(200);
const readinessResponse = http.expectedStatuses(200, 503);

export const options = {
  scenarios: {
    health: {
      executor: 'constant-vus',
      vus: 5,
      duration: '1m',
      exec: 'healthCheck',
    },
    readiness: {
      executor: 'constant-vus',
      vus: 5,
      duration: '1m',
      exec: 'readinessCheck',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000', 'p(99)<2500'],
  },
};

const baseUrl = __ENV.BASE_URL;

if (!baseUrl) {
  throw new Error('BASE_URL is required');
}

export function healthCheck() {
  const response = http.get(`${baseUrl}/health/liveness`, {
    responseCallback: healthResponse,
  });
  check(response, {
    'health status 200': (r) => r.status === 200,
  });
  sleep(1);
}

export function readinessCheck() {
  const response = http.get(`${baseUrl}/health/readiness`, {
    responseCallback: readinessResponse,
  });
  check(response, {
    'readiness status is 200 or 503': (r) => r.status === 200 || r.status === 503,
  });
  sleep(1);
}
