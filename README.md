# Step Only Android

Step Only는 외부 통신 없이 Android 기기 내부 걸음 수 센서와 로컬 저장소만 사용하는 오프라인 만보기 앱입니다.

## 주요 기능

- 오늘 걸음 수와 추정 이동거리 표시
- 앱 설치 이후 전체 누적 걸음 수와 누적 이동거리 표시
- 날짜별 걸음 수 기록 저장
- 사용자가 직접 시작, 일시중지, 중지하는 걷기 세션 기록
- 알림에서 오늘 걸음 수 확인 및 수동 기록 제어
- `TYPE_STEP_COUNTER` 센서 raw 값의 증가분만 누적하는 delta 방식
- 기기 재부팅 또는 센서 기준값 초기화 처리
- Foreground Service 기반 백그라운드 걸음 수 기록
- Room/SQLite 로컬 DB 저장
- 인터넷 권한, 서버, 계정, 클라우드, 외부 헬스 플랫폼 연동 없음

## 기술 스택

- Kotlin
- Jetpack Compose
- Room
- Foreground Service
- SensorManager + `Sensor.TYPE_STEP_COUNTER`
- 최소 지원 버전: Android 12, API 31

## 권한

앱은 다음 권한만 사용합니다.

- `ACTIVITY_RECOGNITION`: 걸음 수 센서 사용
- `FOREGROUND_SERVICE`: 백그라운드 서비스 실행
- `FOREGROUND_SERVICE_HEALTH`: 건강/운동 유형 포그라운드 서비스
- `POST_NOTIFICATIONS`: 백그라운드 실행 알림 표시
- `RECEIVE_BOOT_COMPLETED`: 기기 재부팅 후 서비스 재시작

`INTERNET` 권한은 사용하지 않습니다.

## 데이터 저장 방식

모든 데이터는 기기 내부 Room DB에 저장됩니다.

- `daily_steps`: 날짜별 걸음 수 기록
- `walking_sessions`: 수동 걷기 세션 기록 및 일시중지 상태
- `app_state`: 마지막 센서 raw 값, 전체 누적 걸음 수, 보폭 등 앱 상태값

거리 계산은 기본 보폭 `0.70m`를 사용합니다.

```text
거리(km) = 걸음 수 x 보폭(m) / 1000
```

## 빌드

Android Studio에서 프로젝트를 열거나, JDK가 설정된 환경에서 다음 명령을 실행합니다.

```powershell
.\gradlew.bat :app:assembleDebug
```

디버그 APK는 다음 위치에 생성됩니다.

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub 업로드

권장 저장소 이름:

```text
step-only-android
```

GitHub에서 빈 저장소를 만든 뒤 아래 명령으로 원격 저장소를 연결합니다.

```powershell
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/step-only-android.git
git branch -M main
git push -u origin main
```

이미 `origin`이 있다면 URL만 변경합니다.

```powershell
git remote set-url origin https://github.com/YOUR_GITHUB_USERNAME/step-only-android.git
git push -u origin main
```
