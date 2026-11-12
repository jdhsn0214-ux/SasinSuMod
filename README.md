# 사신수 무기 모드 (Forge 1.20.1)

## 포함된 기능
- 청룡의 검: 우클릭으로 전방 번개/용의 기운 연속 폭발
- 백호의 도끼: 우클릭으로 넓은 다중 참격
- 주작의 단검: 우클릭으로 연속 화염 타격과 폭발
- 현무의 창: 우클릭으로 방어력/흡수 효과와 보호 마법진
- 서버 측 스킬 판정, 무기별 쿨타임과 내구도 소모

## 빌드 환경
- Minecraft 1.20.1
- Minecraft Forge 47.2.0
- Java 17
- Gradle wrapper가 포함되어 있지 않으므로 시스템 Gradle 8.x 또는 IDE의 Gradle 실행 기능 필요

## 빌드
프로젝트 폴더에서 `gradle build` 실행.
결과물: `build/libs/sasinsu-1.0.0.jar`

## 설치
빌드된 JAR를 Minecraft 1.20.1 Forge 프로필의 `mods` 폴더에 넣으세요.

## 조작
각 무기를 손에 들고 우클릭하면 고유 스킬을 사용합니다.
쿨타임은 각각 5초, 4초, 3초, 6초입니다.

## 참고
현재 버전은 플레이 가능한 기능 중심의 1차 구현입니다. 전용 3D 모델/텍스처, 애니메이션, 실제 사신수 소환 엔티티, 커스텀 UI는 후속 작업이 필요합니다.

## 설치 없이 온라인 빌드하기
이 프로젝트에는 GitHub Actions 워크플로가 포함되어 있습니다.
1. ZIP 압축을 풀고 GitHub에 새 저장소를 만듭니다.
2. 압축을 푼 프로젝트 파일과 `.github` 폴더를 저장소에 업로드합니다.
3. 저장소의 Actions 탭에서 `Build Minecraft Forge Mod`를 선택합니다.
4. `Run workflow`를 누릅니다.
5. 작업이 성공하면 실행 기록 아래 Artifacts에서 `sasinsu-forge-1.20.1`을 다운로드합니다.
6. 다운로드한 artifact ZIP 안의 `.jar`를 Forge 1.20.1 `mods` 폴더에 넣습니다.

GitHub Actions가 GitHub 서버에서 Java와 Gradle을 준비하므로 PC에 개발 도구를 설치할 필요가 없습니다.
