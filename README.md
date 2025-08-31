# AI와 다양한 협업툴을 연결해 분산된 정보를 통합하여 제공하는 협업 플러그인 README

<img width="1221" height="682" alt="image" src="https://github.com/user-attachments/assets/1e910be4-8c34-4a7a-a1e5-d9ddd325201c" />


-   배포 URL : https://merging-fe.vercel.app/
-   Test ID : test@gmail.com
-   Test PW : test1234

<br>

## 프로젝트 소개

브랜치파이(Branchify)는 AI와 다양한 협업 툴을 연결해 분산된 정보를 통합 제공하는 협업 플러그인 서비스입니다. 이 서비스는 AI-Agent가 여러 협업 툴에 흩어진 업무 데이터를 통합 관리하고 자동화하며, 문맥을 분석하여 관련 자료를 추천하는 AI 기반 자동화 기능을 제공합니다. 또한 사용자가 필요에 따라 AI-Agent와 협업 툴을 조합해 맞춤형 플러그인 형태로 확장할 수 있습니다.

<br>

## 팀원 구성

<div align="center">

|                                                         **권소현**                                                          |                                                                 **남윤혁**                                                                  |                                                         **박우찬**                                                          |                                                              **유주영**                                                               |
| :-------------------------------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------------------------------------------: |
| [<img src="https://avatars.githubusercontent.com/hyuke81" height=150 width=150> <br/> @hyuke81](https://github.com/hyuke81) | [<img src="https://avatars.githubusercontent.com/namyoonhyeok" height=150 width=150> <br/> @namyoonhyeok](https://github.com/namyoonhyeok) | [<img src="https://avatars.githubusercontent.com/pwc2002" height=150 width=150> <br/> @pwc2002](https://github.com/pwc2002) | [<img src="https://avatars.githubusercontent.com/Juyounge-e" height=150 width=150> <br/> @Juyounge-e](https://github.com/Juyounge-e) |

</div>

<br>

## 1. 개발 환경

-   Front : React, Vite, emotion.js, axios
-   Back-end : Java, Spring Boot, Spring Data JPA, MySQL
-   버전 및 이슈관리 : Github, Github Issues, Github Action
-   협업 툴 : Notion, Discord
    <br>

## 2. 채택한 개발 기술과 브랜치 전략

### Spring Boot, Spring Data JPA & MySQL, Spring Security & JWT

-   Spring Boot

      - 이 프로젝트는 Jira, Notion, Slack 연동, 사용자 인증, 파일 관리(S3) 등 여러 독립적인 기능을 제공하는
        API 서버입니다. Spring Boot의 'starter' 기반 의존성 관리는 spring-boot-starter-web,
        spring-boot-starter-data-jpa, spring-boot-starter-security처럼 각 기능 구현에 필요한 라이브러리들을
        충돌 없이 간편하게 통합해주었습니다.
      - 초기 설정의 복잡성을 대폭 줄여 개발자가 비즈니스 로직에만 집중할 수 있는 환경을 제공합니다.
      - 내장 Tomcat 서버를 활용하여 복잡한 외부 WAS 설정 없이도 개발 초기 단계부터 빠르게
        서버를 실행하고 API를 테스트하며 개발 생산성을 극대화할 수 있었습니다.

-   Spring Data JPA & MySQL

      -  Spring Data JPA의 Repository  인터페이스를 활용하여 각 도메인에 필요한 CRUD 로직을
        보일러플레이트 코드 없이 자동으로 구현했습니다. 이를 통해 SQL 쿼리 작성보다 객체지향적인 
        비즈니스 로직에 집중할 수 있었습니다.
      - 이렇게 생성된 여러 테이블 간의 관계를 명확하게 설정하고, 트랜잭션을 통해 데이터의 무결성과 일관성을 보장하기 위해 신뢰성
        높은 관계형 데이터베이스인 MySQL을 최종 데이터 저장소로 채택했습니다.

-   Spring Security & JWT

    -   사용자의 인증 정보뿐만 아니라 외부 서비스 연동을 위한 민감한 OAuth 토큰을 다루기 때문에
  강력한 보안이 필수적이었습니다. Spring Security의 보안 아키텍처를 적용하여 모든 API
  요청에 대한 인증 및 인가 절차를 체계적으로 관리하고 엔드포인트를 보호했습니다.
    -  Stateless 인증 방식을 구현하기 위해 채택했습니다. 서버가 클라이언트의 세션 상태를 저장할 필요가
         없어 확장성이 뛰어나고, 다양한 클라이언트 환경에 유연하게 대응할 수 있습니다.

<br>

## 3. 프로젝트 구조

```
.
├── build.gradle                
├── settings.gradle
├── .gitIgnore
├── gradlew                     
└── src/                       
   ├── main/java/com/ example/merging/
   │   ├── MergingApplication.java     
   │   │
   │   ├── config/                     
   │   │   └── SecurityConfig.java     
   │   │  
   │   ├── jwt/                       
   │   │   ├── JwtAuthenticationFilter.java 
   │   │   └── JwtTokenProvider.java     
   │   │
   │   ├── user/                       
   │   │   ├── User.java
   │   │   └── UserDTO.java
   │   │   ├── UserRepository.java     
   │   │   ├── UserService.java        
   │   │   └── UserController.java
   │   │   └── RefreshToken.java
   │   │   └── RefreshTokenRepository.java
   |   |
   │   ├──assistantList/                       
   │   │   ├── AssistantList.java               
   │   │   ├── AssistantListRepository.java     
   │   │   ├── AssistantListService.java        
   │   │   └── AssistantListController.java   
   │   │           
   │   ├── notionOAuth/
   │   │   ├── NotionOAuth.java
   │   │   └── NotionOAuthDTO.java
   │   │   ├── AuthorizationCodeDTO.java
   │   │   ├── NotionTokenResponseDTO.java
   │   │   ├── NotionOAuthRepository.java     
   │   │   ├── NotionOAuthService.java        
   │   │   └── NotionOAuthController.java 
   │   │      
   │   ├── slackOAuth/
   │   │   ├── SlacknOAuth.java
   │   │   ├── SlacknOAuthDTO.java
   │   │   ├── SlacknOAuthRepository.java
   │   │   ├── SlacknOAuthService.java
   │   │   ├── SlackOAuthController.java 
   │   │
   │   ├── jiraOAuth/
   │   │   ├── JiraAuthController.java 
   │   │   └── ...
   │   │
   │   ├── s3/                         
   │   │   ├── S3Controller.java
   │   │   └── S3Service.java
   │   │
   │   └── converter/                  
   │   |   └── StringListConverter.java
   │   │
   │   └── resources/                  
   │       ├── application.yml
   │       ├── static/                 
   │       └── templates/             
   │
   └── test/java/com/example/merging/               
       └── MergingApplicationTests.java 
           
```

<br>

## 4. 개발 기간

### 개발 기간

-   전체 개발 기간 : 2025-01-04 ~ 2025-02-28

<br>

### 5. 사용자 기능

-   AI 기반 검색 기능

    -   문서, 대화 맥락을 기반으로 AI가 필요한 정보를 검색하여 제공합니다.

-   대화 요약 및 정리 기능

    -   채팅형 서비스(Slack 등)의 대화 내용을 요약하고, 회의 내용, 주요 결정사항, 액션 아이템을 자동으로 정리해 줍니다.

-   AI 기반 업무 자동화

    -   일정 알림, 태스크 업데이트와 같은 반복적인 업무를 AI가 자동으로 처리하여 사용자의 업무 효율을 높여줍니다.

-   AI 챗봇 기능

    -   메신저의 챗봇에 질문하면, 여러 협업 툴에서 관련된 정보를 자동으로 찾아 답변해 줍니다.

-   통합된 업무 환경 제공

    -   Notion, Jira, PDF 문서 등 다양한 협업 툴과 연동하여 채팅형 SaaS에서 모든 업무를 한 곳에서 관리할 수 있습니다.

-   간단한 설정 및 직관적인 UI - 경쟁 서비스에 비해 설정이 간단하고, 기존 협업 툴 인터페이스에서 바로 도입할 수 있어 누구나 쉽게 사용할 수 있습니다.
    <br>
