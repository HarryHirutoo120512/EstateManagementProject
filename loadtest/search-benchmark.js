import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '15s', target: 20 },   // làm nóng
    { duration: '30s', target: 50 },   // ổn định
    { duration: '30s', target: 100 },  // đỉnh: tìm điểm bão hòa
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    // Ngưỡng chỉ để tham khảo: chỉnh theo máy của bạn
    http_req_duration: ['p(50)<100', 'p(95)<300', 'p(99)<600'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

// 80% truy vấn "nóng" lặp lại, 20% ngẫu nhiên => đo được cả cache hit lẫn miss
const HOT = [
  'district=QUAN_1&rentPriceFrom=10&rentPriceTo=50&areaFrom=100&areaTo=300',
  'district=QUAN_2&rentPriceFrom=20&rentPriceTo=60&typeCode=NGUYEN_CAN',
  'district=QUAN_3&areaFrom=200&areaTo=500',
  'name=Tower&district=QUAN_1',
  'district=QUAN_4&rentPriceFrom=15&rentPriceTo=40',
];

export default function () {
  const hot = Math.random() < 0.8;
  const page = hot ? 0 : Math.floor(Math.random() * 5); // Spring Pageable: page bắt đầu từ 0
  const query = hot
    ? HOT[Math.floor(Math.random() * HOT.length)]
    : `district=QUAN_${1 + Math.floor(Math.random() * 4)}&rentPriceFrom=${10 + Math.floor(Math.random() * 40)}`;

  const res = http.get(`${BASE}/api/building?${query}&page=${page}&size=20`);
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(0.1);
}
