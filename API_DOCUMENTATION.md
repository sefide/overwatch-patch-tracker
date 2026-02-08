# 🎮 Overwatch Patch Tracker API Documentation

Base URL: `http://localhost:8080` (로컬) 또는 배포된 서버 주소

## 📚 API Endpoints

### 1. 영웅 관련 API

#### 1.1 모든 영웅 목록 조회
```http
GET /api/heroes
```

**Response:**
```json
{
  "count": 41,
  "heroes": [
    "Ashe",
    "Bastion",
    "D.Va",
    ...
  ]
}
```

#### 1.2 특정 영웅의 업데이트 조회
```http
GET /api/heroes/{heroName}?limit=10
```

**Parameters:**
- `heroName` (path): 영웅 이름 (예: "Ashe", "D.Va")
- `limit` (query, optional): 최대 결과 개수

**Example:**
```bash
curl http://localhost:8080/api/heroes/Ashe
```

**Response:**
```json
{
  "heroName": "Ashe",
  "totalUpdates": 5,
  "updates": [
    {
      "id": 123,
      "heroName": "Ashe",
      "patchDate": "2026-01-20",
      "patchVersion": "Patch January 20, 2026",
      "developerComment": "The increased falloff range helped...",
      "changes": [
        {
          "abilityName": "The Viper",
          "changeType": "NERF",
          "description": "Secondary fire falloff range reduced from 40-60 meters to 35-55 meters",
          "previousValue": "40-60",
          "newValue": "35-55",
          "unit": "meters"
        }
      ]
    }
  ]
}
```

---

### 2. 패치 관련 API

#### 2.1 모든 패치 목록 조회
```http
GET /api/patches?startDate=2025-01-01&endDate=2026-01-31&limit=50
```

**Parameters:**
- `startDate` (query, optional): 시작 날짜 (YYYY-MM-DD)
- `endDate` (query, optional): 종료 날짜 (YYYY-MM-DD)
- `limit` (query, optional, default=50): 최대 결과 개수

**Example:**
```bash
curl "http://localhost:8080/api/patches?limit=10"
```

**Response:**
```json
{
  "count": 10,
  "patches": [
    {
      "date": "2026-01-20",
      "version": "Patch January 20, 2026",
      "heroCount": 12,
      "heroes": ["Ashe", "Bastion", "Doomfist", ...]
    }
  ]
}
```

#### 2.2 최신 패치 조회
```http
GET /api/patches/latest
```

**Example:**
```bash
curl http://localhost:8080/api/patches/latest
```

**Response:**
```json
{
  "date": "2026-01-20",
  "version": "Patch January 20, 2026",
  "heroCount": 12,
  "updates": [
    {
      "heroName": "Ashe",
      "patchDate": "2026-01-20",
      ...
    }
  ]
}
```

---

### 3. 통계 API

#### 3.1 영웅별 버프/너프 통계
```http
GET /api/stats/buffs-nerfs?startDate=2025-01-01&endDate=2026-01-31
```

**Parameters:**
- `startDate` (query, optional): 시작 날짜
- `endDate` (query, optional): 종료 날짜

**Example:**
```bash
curl http://localhost:8080/api/stats/buffs-nerfs
```

**Response:**
```json
{
  "totalHeroes": 41,
  "dateRange": null,
  "stats": [
    {
      "heroName": "Ashe",
      "totalChanges": 15,
      "buffs": 5,
      "nerfs": 8,
      "adjustments": 1,
      "bugFixes": 1,
      "updateCount": 7
    }
  ]
}
```

---

### 4. 스크래핑 API

#### 4.1 최신 패치 스크래핑
```http
POST /api/scrape/latest
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/scrape/latest
```

**Response:**
```json
{
  "success": true,
  "message": "Successfully scraped and saved 12 hero updates",
  "savedCount": 12
}
```

#### 4.2 특정 월 스크래핑
```http
POST /api/scrape/month?year=2026&month=1
```

**Parameters:**
- `year` (query): 연도 (2016-2030)
- `month` (query): 월 (1-12)

**Example:**
```bash
curl -X POST "http://localhost:8080/api/scrape/month?year=2026&month=1"
```

**Response:**
```json
{
  "success": true,
  "message": "Successfully scraped 2026-1 and saved 45 hero updates",
  "savedCount": 45
}
```

---

### 5. 헬스체크 API

#### 5.1 서버 상태 확인
```http
GET /api/health
```

**Example:**
```bash
curl http://localhost:8080/api/health
```

**Response:**
```json
{
  "status": "UP",
  "totalUpdates": 523,
  "latestPatchDate": "2026-01-20"
}
```

---

## 🔍 사용 예시

### 특정 영웅의 최근 3개 업데이트만 가져오기
```bash
curl "http://localhost:8080/api/heroes/Genji?limit=3"
```

### 2025년 12월 패치만 조회
```bash
curl "http://localhost:8080/api/patches?startDate=2025-12-01&endDate=2025-12-31"
```

### 2026년 1월 데이터 스크래핑 후 통계 확인
```bash
# 1. 스크래핑
curl -X POST "http://localhost:8080/api/scrape/month?year=2026&month=1"

# 2. 통계 조회
curl "http://localhost:8080/api/stats/buffs-nerfs?startDate=2026-01-01&endDate=2026-01-31"
```

---

## 🚨 에러 응답

모든 API는 에러 발생 시 적절한 HTTP 상태 코드와 함께 응답합니다:

- `200 OK`: 성공
- `400 Bad Request`: 잘못된 요청 (예: 잘못된 날짜 형식)
- `404 Not Found`: 리소스를 찾을 수 없음
- `500 Internal Server Error`: 서버 오류

**Error Response Example:**
```json
{
  "timestamp": "2026-01-20T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid year or month",
  "path": "/api/scrape/month"
}
```

---

## 📊 데이터 타입

### ChangeType (변경 타입)
- `BUFF`: 강화/증가
- `NERF`: 약화/감소
- `ADJUSTMENT`: 일반 조정
- `BUG_FIX`: 버그 수정
- `NEW_ABILITY`: 새로운 능력

### 날짜 형식
모든 날짜는 ISO 8601 형식 사용: `YYYY-MM-DD`

예: `2026-01-20`

---

## 🔐 CORS 설정

현재 모든 오리진(`*`)에서 접근 가능하도록 설정되어 있습니다.
프로덕션 환경에서는 특정 도메인만 허용하도록 변경 필요:

```kotlin
@CrossOrigin(origins = ["https://yourdomain.com"])
```

---

## 🛠️ 테스트

### Postman Collection
[추후 추가 예정]

### cURL 예제 모음
```bash
# 전체 영웅 목록
curl http://localhost:8080/api/heroes

# 최신 패치
curl http://localhost:8080/api/patches/latest

# 버프/너프 통계
curl http://localhost:8080/api/stats/buffs-nerfs

# 헬스체크
curl http://localhost:8080/api/health
```
