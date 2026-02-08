# 🎮 Overwatch Patch Tracker

오버워치 2의 영웅별 패치 노트(버프/너프)를 자동으로 수집하고 추적하는 웹 애플리케이션

## 📋 주요 기능

- ✅ Blizzard 공식 패치 노트 자동 스크래핑
- ✅ 영웅별 변경사항 추적 (버프/너프/조정/버그수정)
- ✅ 날짜별, 영웅별 필터링
- ✅ 수치 변화 자동 추출 및 분류
- ✅ 개발자 코멘트 포함

## 🛠️ 기술 스택

### Backend
- **언어**: Kotlin
- **프레임워크**: Spring Boot 3.2.1
- **ORM**: Spring Data JPA + Hibernate
- **웹 스크래핑**: Jsoup 1.17.2
- **데이터베이스**: PostgreSQL (Production), H2 (Test)

### Testing
- Kotest 5.8.0
- Spring Boot Test

## 📁 프로젝트 구조

```
overwatch-patch-tracker/
├── src/
│   ├── main/
│   │   ├── kotlin/com/overwatch/patchtracker/
│   │   │   ├── OverwatchPatchTrackerApplication.kt  # 메인 애플리케이션
│   │   │   ├── domain/
│   │   │   │   └── Models.kt                       # HeroUpdate, BalanceChange 엔티티
│   │   │   ├── repository/
│   │   │   │   └── HeroUpdateRepository.kt         # JPA Repository
│   │   │   ├── service/
│   │   │   │   ├── OverwatchPatchScraper.kt       # 웹 스크래핑 로직
│   │   │   │   └── OverwatchPatchService.kt       # 비즈니스 로직
│   │   │   └── dto/
│   │   │       └── PatchDtos.kt                   # DTO 클래스들
│   │   └── resources/
│   │       └── application.yml                     # 설정 파일
│   └── test/
│       └── kotlin/com/overwatch/patchtracker/
│           └── OverwatchPatchScraperTest.kt       # 테스트 코드
├── build.gradle.kts                                # Gradle 빌드 설정
└── README.md
```

## 🚀 로컬 실행

### 필수 요구사항
- JDK 17 이상
- Gradle 8.5+
- PostgreSQL (또는 H2 사용)

### 1. 프로젝트 클론

```bash
git clone https://github.com/yourusername/overwatch-patch-tracker.git
cd overwatch-patch-tracker
```

### 2. 빌드

```bash
./gradlew build
```

### 3. 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 스크래퍼 테스트만
./gradlew test --tests "OverwatchPatchScraperTest"
```

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

## 🗄️ 데이터베이스 스키마

### HeroUpdate (영웅 업데이트)
```sql
CREATE TABLE hero_updates (
    id BIGSERIAL PRIMARY KEY,
    hero_name VARCHAR(255) NOT NULL,
    patch_date DATE NOT NULL,
    patch_version VARCHAR(255) NOT NULL,
    developer_comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### BalanceChange (밸런스 변경사항)
```sql
CREATE TABLE balance_changes (
    id BIGSERIAL PRIMARY KEY,
    hero_update_id BIGINT REFERENCES hero_updates(id),
    ability_name VARCHAR(255),
    change_type VARCHAR(50) NOT NULL, -- BUFF, NERF, ADJUSTMENT, BUG_FIX
    description TEXT NOT NULL,
    previous_value VARCHAR(100),
    new_value VARCHAR(100),
    unit VARCHAR(50)
);
```

## 📊 데이터 수집 예시

스크래핑 결과 예시:

```json
{
  "date": "2026-01-20",
  "version": "Patch January 20, 2026",
  "heroUpdates": [
    {
      "heroName": "Ashe",
      "developerComment": "The increased falloff range helped reinforce Ashe's role...",
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

## 🌐 배포 옵션

### 무료 호스팅 옵션

1. **Backend (Spring Boot)**
   - [Railway](https://railway.app/) - 무료 티어 제공
   - [Render](https://render.com/) - 무료 티어 제공
   - [Fly.io](https://fly.io/) - 무료 티어 제공

2. **Database**
   - Railway PostgreSQL (무료)
   - [Supabase](https://supabase.com/) (무료)
   - [ElephantSQL](https://www.elephantsql.com/) (무료)

3. **Frontend (추후 추가 시)**
   - [Vercel](https://vercel.com/)
   - [Netlify](https://www.netlify.com/)
   - GitHub Pages

### Railway 배포 예시

1. Railway 계정 생성
2. 프로젝트 연결:
   ```bash
   railway login
   railway init
   railway link
   ```
3. 환경 변수 설정:
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://...
   SPRING_DATASOURCE_USERNAME=...
   SPRING_DATASOURCE_PASSWORD=...
   ```
4. 배포:
   ```bash
   railway up
   ```

## 📝 API 엔드포인트 (예정)

```
GET  /api/heroes                    # 모든 영웅 목록
GET  /api/heroes/{name}/updates     # 특정 영웅의 업데이트 이력
GET  /api/patches                   # 모든 패치 목록
GET  /api/patches/latest            # 최신 패치
GET  /api/stats/heroes              # 영웅별 버프/너프 통계
```

## 🔄 자동 크롤링 (예정)

GitHub Actions를 사용한 주기적 크롤링:

```yaml
# .github/workflows/scrape.yml
name: Scrape Patches
on:
  schedule:
    - cron: '0 12 * * *'  # 매일 12시
  workflow_dispatch:

jobs:
  scrape:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: ./gradlew bootRun --args='--scrape'
```

## ⚖️ 법적 고려사항

- 이 프로젝트는 **교육/개인 목적**으로 제작되었습니다
- 모든 데이터는 Blizzard Entertainment의 공개 패치 노트에서 수집됩니다
- 출처 표시: "Data from Blizzard Entertainment"
- 상업적 사용 전 법적 자문 권장

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

MIT License

## 👤 개발자

- GitHub: [@yourusername](https://github.com/yourusername)

## 🙏 감사의 말

- Blizzard Entertainment - 오버워치 2
- Jsoup - HTML 파싱 라이브러리
