# AI 공부친구 JANE

초등학생을 위한 AI 학습 도우미 안드로이드 앱.
수학·영어 문제를 사진으로 찍으면 AI(Gemini/Claude)가 채점하고, 왜 틀렸는지 설명하고, 비슷한 연습문제를 내준다.

## 주요 기능
- 📷 **문제 사진 분석** — 수학/영어 문제를 찍으면 채점 + "왜 틀렸는지" + 단계별 풀이
- ✍️ **손글씨 채점** — 연습문제 답을 손가락으로 쓰면 AI가 읽어서 채점 (전체화면 입력)
- 📚 **영어 단어 학습** — 단어장 / 암기카드 / 퀴즈 / 📷 사진퀴즈(사진 단어 → 랜덤 손글씨 퀴즈)
- 📕 **오답노트** — 틀린 문제만 모아보기
- 🔊 영어 발음(TTS), 🎉 정답 시 축하 연출 + 진동
- AI 엔진: Gemini(무료) / Claude(유료) 선택, 앱 내 키 입력

## 빌드
시스템 Gradle 사용 (래퍼 없음):
```
gradle assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
- JDK 17, Android SDK (compileSdk 34), Kotlin 1.9.24 + Jetpack Compose
- `local.properties`에 `sdk.dir` 설정 필요 (git에는 안 올라감)

## 사용
앱 ⚙️ 설정에서 Gemini API 키 입력 (aistudio.google.com 무료 발급). 키는 기기에만 저장되며 코드/저장소에 없음.

## 구조
| 파일 | 역할 |
|------|------|
| `MainActivity.kt` | 메인 UI (홈/분석결과/설정/기록/오답노트) |
| `VocabScreen.kt` | 영어 단어 학습 (단어장/암기/퀴즈/사진퀴즈) |
| `AiClient.kt` | Gemini/Claude 비전 클라이언트 + 프롬프트 |
| `HandwritingAnswer.kt` | 전체화면 손글씨 입력 + AI 채점 |
| `Imaging.kt` | 이미지/손글씨 공용 유틸 |
| `Celebration.kt` | 정답 축하 연출 + 진동 |
| `Models.kt` / `Store.kt` / `WordBank.kt` | 데이터 모델 / 저장 / 단어 데이터 |
