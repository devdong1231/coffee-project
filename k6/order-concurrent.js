import http from 'k6/http';
import { check, fail, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_IDS = parseIds(__ENV.USER_IDS);
const MENU_IDS = parseIds(__ENV.MENU_IDS);
const QUANTITY = Number(__ENV.QUANTITY || '1');

export const options = {
  scenarios: {
    concurrent_orders: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || '10'),
      duration: __ENV.DURATION || '1m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000'],
  },
};

export default function () {
  if (USER_IDS.length === 0 || MENU_IDS.length === 0) {
    fail('USER_IDS and MENU_IDS must be provided. Example: USER_IDS=1,2 MENU_IDS=1,2');
  }

  const userId = USER_IDS[(__VU - 1) % USER_IDS.length];
  const menuId = MENU_IDS[__ITER % MENU_IDS.length];
  const payload = JSON.stringify({
    userId,
    items: [
      {
        menuId,
        quantity: QUANTITY,
      },
    ],
  });

  const response = http.post(`${BASE_URL}/api/orders`, payload, {
    headers: {
      'Content-Type': 'application/json',
    },
  });

  check(response, {
    'order status is 201': (res) => res.status === 201,
    'order api status is 201': (res) => res.status === 201 && res.json('status') === 201,
    'order id exists': (res) => res.status === 201 && Number.isInteger(res.json('data.orderId')),
  });

  sleep(1);
}

function parseIds(value) {
  if (!value) {
    return [];
  }

  return value
    .split(',')
    .map((id) => Number(id.trim()))
    .filter((id) => Number.isInteger(id) && id > 0);
}
