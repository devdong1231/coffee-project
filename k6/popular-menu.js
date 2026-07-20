import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    popular_menu: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 30 },
        { duration: '1m', target: 30 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<300'],
  },
};

export default function () {
  const response = http.get(`${BASE_URL}/api/menus/popular`);

  check(response, {
    'popular menu status is 200': (res) => res.status === 200,
    'popular menu api status is 200': (res) => res.status === 200 && res.json('status') === 200,
  });

  sleep(1);
}
