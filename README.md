# 2026-1-CECD1-3-BreathAI-05

## BreathAI
**AI 기반 룰기반 챗봇 학습데이터 자동 현행화 시스템**

서울교통공사 챗봇(또타24)의 상담 로그를 AI로 분석해 미답변·오답변 질문을 찾고, 신규 FAQ 후보와 기존 FAQ 확장안(키워드·유사어)을 자동으로 도출합니다. 운영자는 웹 화면에서 후보를 검토·승인해 운영 FAQ에 반영하고, 반영 이력을 버전으로 관리합니다.

> ### 개발 저장소 안내
> 이 저장소는 종합설계 공통 저장소입니다. 방학 중에도 개발을 이어가기 위해 **실제 개발은 별도 저장소에서 진행했습니다.**
>
> - 개발 저장소: https://github.com/yujin110/BreathAI
>   - 최신 개발 코드: `develop` 브랜치 / 최종본: `main` 브랜치
>   - 이슈·PR·커밋 이력과 설계·검증 문서도 모두 개발 저장소에 있습니다.
> - 서비스 화면: https://yujin110.github.io/BreathAI/ (서울교통공사 메일 인증 후 이용)

<br>

## 주요 기능

**로그 분석**
- 상담 로그(xlsx·csv, 최대 50MB) 업로드
- 분석 기간·대상(미답변+오답변 / 미답변만 / 전체) 선택, 개인정보 마스킹
- 진행 단계 실시간 표시, 분석 취소

**분석 결과**
- 응답 품질 분류(정답·오답변·미답변)
- 질문 군집 시각화, 기간별 추이와 급증 키워드 대시보드

**FAQ 후보 검토**
- 신규 FAQ 후보와 기존 FAQ 확장 후보
- 답변 초안·키워드·유사어 제안, 승인·수정·반려

**FAQ 관리**
- FAQ 등록·수정·삭제, 카테고리 관리
- 변경 시 버전 자동 생성, 버전 비교·복원
- CSV 다운로드·일괄 업로드, 키워드 다운로드

**계정·보안**
- 공사 메일 인증 회원가입, 관리자 권한, JWT 로그인
- API 감사로그
<br>

## 시스템 구성

```
[운영자 브라우저]
      │
[프론트엔드]  React · GitHub Pages
      │ HTTPS
[백엔드]  Spring Boot · NCP 서버(Docker, Nginx) ── [MariaDB]
      │                                       └─ [NCP Object Storage] 업로드 원본 · DB 백업
      │ SSH 터널 (분석 요청 / 결과 콜백)
[AI 분석 서버]  FastAPI · Vast.ai GPU
```

- 백엔드는 업로드를 받아 AI 서버에 분석을 요청하고, AI 서버는 분석이 끝나면 결과를 백엔드로 보냅니다(콜백).
- 백엔드와 AI 서버 사이는 SSH 터널 하나로 연결해, 서버의 내부 포트를 외부에 열지 않습니다.

<br>

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Frontend | React 19, Vite, JavaScript, Recharts |
| Backend | Java 17, Spring Boot 4, Spring Security(JWT), Spring Data JPA, Flyway |
| AI Server | Python 3.11, FastAPI, PyTorch, sentence-transformers, UMAP, HDBSCAN, Llama 3.1 8B Instruct, Kiwi(형태소 분석) |
| Database | MariaDB 11.4 |
| Infra | NCP 서버(Docker Compose, Nginx), NCP Object Storage, Vast.ai GPU, GitHub Pages·Actions |

<br>

## 개발 저장소 구성

| 폴더 | 내용 |
| --- | --- |
| `ttobagi-front/` | 프론트엔드(React) |
| `ttobagi-server/` | 백엔드(Spring Boot), 배포 설정, API 명세 변경 문서 |
| `services/` | AI 분석 서버와 파이프라인(FastAPI) |
| `db/schema/` | AI 서버가 관리하는 원천(Bronze) 테이블 정의 |
| `evaluation/`, `tests/` | AI 품질 평가와 테스트 |
| `docs/` | 설계·검증 기록 |

<br>

## 팀원

| 이름 | 역할 |
| --- | --- |
| 박서연 (팀장) | Backend |
| 김시환 | AI / AI Server |
| 오유진 | Frontend |

<br>

## 진행

- 2026년 1학기: 종합설계1 – 요구사항 정의, 시스템 설계, 프로토타입 개발
- 2026년 여름방학 ~ 2학기: 종합설계2 –  서버 배포, 서울교통공사 담당자 시험 및 보완
