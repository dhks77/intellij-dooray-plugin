# IntelliJ Dooray Plugin

IntelliJ IDEA에서 Dooray 업무 관리를 더욱 효율적으로! 
Git 브랜치명에서 업무 번호를 자동으로 인식하고, Dooray 업무 정보를 쉽게 확인할 수 있는 플러그인입니다.

## ✨ 주요 기능

### 📊 Status Bar Widget
- 현재 브랜치의 Dooray 업무 정보를 상태바에 표시
- 클릭하면 브라우저에서 해당 업무 페이지를 자동으로 열기
- 업무 번호와 제목을 한눈에 확인

### 🌲 스마트 브랜치 관리
- **브랜치 그룹화**: 브랜치를 prefix별로 자동 분류 (Feature/, Coldfix/, Hotfix/ 등)
- **업무 정보 표시**: 각 브랜치의 Dooray 업무 번호와 제목 표시
- **3단계 네비게이션**: 그룹 선택 → 액션 선택 → 브랜치 선택

### 🔄 배치 작업
- **모든 업무 불러오기**: 선택한 그룹의 모든 브랜치 업무 정보를 한 번에 캐시에 저장
- **다중 브랜치 삭제**: 체크박스로 여러 브랜치를 선택해서 일괄 삭제
- **진행률 표시**: 장시간 작업 시 진행률과 취소 기능 제공

### 💾 스마트 캐시 시스템
- **영구 캐시**: IntelliJ 재시작 후에도 업무 정보 유지
- **자동 만료**: 30일 후 자동으로 캐시 데이터 만료
- **성능 최적화**: API 호출 최소화로 빠른 응답 속도

### ⌨️ 키보드 단축키
- **Windows/Linux**: 
  - `Ctrl+Alt+D`: 현재 브랜치 업무 페이지 열기
  - `Ctrl+Alt+Shift+D`: 브랜치 선택 메뉴 열기
- **Mac**: 
  - `Ctrl+Cmd+Shift+D`: 현재 브랜치 업무 페이지 열기
  - `Ctrl+Cmd+Shift+S`: 브랜치 선택 메뉴 열기

## 🚀 사용 방법

### 1. 초기 설정
1. `Settings/Preferences` → `Dooray Settings` 메뉴 접근
2. 다음 정보 입력:
   - **Domain**: Dooray 도메인 (예: `https://your-company.dooray.com`)
   - **Token**: Dooray API 토큰
   - **Project ID**: 프로젝트 ID

### 2. 브랜치 명명 규칙
플러그인이 자동으로 인식하려면 브랜치명 마지막에 업무 번호가 있어야 합니다:
```
✅ 올바른 예시:
- feature/1234
- hotfix/5678
- develop/9012

❌ 잘못된 예시:
- feature/1234-login-page
- hotfix/general-bug-fix
- develop/new-feature
```

### 3. 주요 사용 시나리오

#### 📖 현재 브랜치 업무 확인
- 상태바에서 업무 정보 확인
- 클릭으로 브라우저에서 업무 페이지 열기
- 키보드 단축키로 빠른 접근

#### 🔍 다른 브랜치 업무 탐색
1. 키보드 단축키로 브랜치 선택 메뉴 열기
2. 원하는 브랜치 그룹 선택
3. **브랜치 목록 보기** 선택
4. 특정 브랜치의 업무 페이지 열기

#### 📥 업무 정보 일괄 로드
1. 브랜치 선택 메뉴에서 그룹 선택
2. **모든 업무 불러오기** 선택
3. 해당 그룹의 모든 브랜치 업무 정보가 캐시에 저장됨

#### 🗑️ 브랜치 일괄 삭제
1. 브랜치 선택 메뉴에서 그룹 선택
2. **여러 브랜치 삭제** 선택
3. 체크박스로 삭제할 브랜치들 선택
4. 확인 후 일괄 삭제 실행

## 🛠️ 설치 방법

### 수동 설치
1. [최신 릴리즈](https://github.com/dhks77/intellij-dooray-plugin/releases/latest)에서 플러그인 파일 다운로드
2. `Settings/Preferences` → `Plugins` → `⚙️` → `Install plugin from disk...`
3. 다운로드한 파일 선택

## 🔧 개발 환경 요구사항

- IntelliJ IDEA 2023.1 이상
- Git 저장소
- Java 21 이상 (개발 시)
- Kotlin 1.9 이상 (개발 시)

## 🙏 감사의 말

이 플러그인은 [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)을 기반으로 개발되었습니다.

---

**💡 팁**: 브랜치명에 업무 번호를 포함시키면 더욱 효율적인 업무 관리가 가능합니다!
