// k6 load generator script for metrics workshop
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

// Configuration
export const options = {
  // Run with constant load
  scenarios: {
    constant_load: {
      executor: 'constant-vus',
      vus: 5,
      duration: '24h', // Run continuously
    },
  },
  // Thresholds (optional, for reporting)
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    errors: ['rate<0.1'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://app:8080';

// Request distribution: 80% fast, 15% slow, 5% error
export default function () {
  const rand = Math.random();
  let response;
  let endpoint;

  if (rand < 0.80) {
    // 80% - Fast endpoint
    endpoint = '/fast';
    response = http.get(`${BASE_URL}${endpoint}`);
  } else if (rand < 0.95) {
    // 15% - Slow endpoint
    endpoint = '/slow';
    response = http.get(`${BASE_URL}${endpoint}`);
  } else {
    // 5% - Error endpoint
    endpoint = '/error';
    response = http.get(`${BASE_URL}${endpoint}`);
  }

  // Check response
  const success = check(response, {
    'status is 2xx or 5xx expected': (r) => r.status === 200 || r.status === 500,
  });

  // Track errors (5xx responses)
  errorRate.add(response.status >= 500);

  // Small pause between requests (100-300ms)
  sleep(0.1 + Math.random() * 0.2);
}

