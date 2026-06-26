# JobRadar AWS Free Tier 운영환경 이관 계획

> 이 문서를 배포 마이그레이션의 단일 기준 문서로 사용한다. 각 단계가 끝날 때 완료 현황, 실제 설정값, 검증 결과와 다음 작업을 갱신한다.

## 0. 현재 진행 현황

기준일: `2026-06-27`

운영 트래픽은 신규 AWS 계정의 ALB + EC2 Docker 백엔드로 전환됐다. 프론트엔드는 기존 AWS 계정의 S3 + CloudFront를 계속 사용하며, API 호출만 `https://api.jobradar.me`를 통해 신규 계정 ALB로 전달한다.

현재 상태는 “신규 인프라 이관과 Docker 자동 배포 완료” 단계다. 엄밀한 의미의 ALB Target Group 기반 Blue/Green 무중단 배포는 아직 완료되지 않았고, 다음 작업에서 “배포 중에만 임시 EC2를 추가 생성하는 EC2 단위 Blue/Green” 방식으로 구현한다.

### 0.1 확정 정보

```text
GitHub Repository: sungjiRyu/jobradar
신규 AWS Account: 458697684210
Region: ap-northeast-2
ECR Repository: jobradar-backend
현재 Backend Image Tag: c3a1ef6445cf112cd9feea11fa93febf93922868
Backend API Domain: https://api.jobradar.me
Frontend Domain: https://jobradar.me
현재 운영 방식: ALB + 단일 EC2 Docker 컨테이너
현재 배포 방식: GitHub Actions + ECR + SSM Run Command 자동 배포
다음 목표 운영 방식: ECR + Docker + 배포 중 임시 EC2 + ALB Blue/Green
```

비용 원칙:

- 실무와 유사한 VPC, Private Data Subnet, ALB, Blue/Green 구조를 유지한다.
- RDS는 Single-AZ를 사용한다.
- Valkey는 ElastiCache Serverless를 사용한다.
- 평상시 Backend EC2는 1대만 유지하고, Blue/Green 배포 중에만 임시 EC2를 추가 생성한다.
- NAT Gateway와 유료 Interface VPC Endpoint는 사용하지 않는다.
- 신규 계정의 `$200` 크레딧을 사용하며, 유료 리소스 생성 후에는 작업을 연속 진행해 유휴 비용을 줄인다.

### 0.2 완료된 작업

- [x] 신규 계정 IAM 관리자와 Billing 접근 설정
- [x] Free Tier 및 Billing 알림 설정
- [x] JobRadar 전용 VPC 생성
- [x] 서로 다른 AZ의 Public Subnet 2개와 Private Subnet 2개 생성
- [x] Internet Gateway와 Public/Private Route Table 구성
- [x] ALB, Backend, RDS, Valkey Security Group 생성
- [x] RDS와 ElastiCache Private Subnet Group 생성
- [x] ECR Private Repository `jobradar-backend` 생성
- [x] ECR Immutable 태그, AES-256, Basic scan on push 설정
- [x] Untagged 이미지 1일 후 삭제 Lifecycle Rule 설정
- [x] Java 21 멀티 스테이지 Dockerfile과 `.dockerignore` 추가
- [x] 로컬 MySQL 8.0, Valkey 8, Backend 통합 실행 검증
- [x] 비루트 `spring` 사용자 실행과 이미지 내 비밀값 미포함 확인
- [x] `/actuator/health` 공개 및 상세 정보 비노출 설정
- [x] 신규 계정 GitHub OIDC Provider와 ECR Push IAM Role 생성
- [x] GitHub Repository Variables 등록
- [x] GitHub Actions에서 `linux/amd64` 이미지 빌드와 ECR Push 성공
- [x] 동일 Git SHA 이미지가 이미 있으면 Push를 생략하도록 멱등성 적용
- [x] 기존 JAR 배포와 운영 서비스 정상 동작 확인
- [x] 신규 RDS MySQL `database-1` 생성
- [x] 기존 RDS `job_radar` 데이터를 `mysqldump`로 신규 RDS에 이관
- [x] ElastiCache Serverless Valkey `jobradar-valkey` 생성
- [x] SSM Parameter Store에 운영 환경변수와 Vertex 인증정보 등록
- [x] Green EC2 `jobradar-green-ec2` 생성
- [x] Green EC2에서 ECR image pull, Docker container 실행, RDS·Valkey·Vertex 연결 검증
- [x] Valkey Serverless TLS 연결을 위해 `REDIS_SSL=true`와 `rediss://` 지원 추가
- [x] ALB `jobradar-alb` 생성
- [x] Target Group `jobradar-green-tg` health check `healthy` 확인
- [x] ACM 인증서 `jobradar.me`, `*.jobradar.me` 발급
- [x] 기존 계정 Route 53에 `api.jobradar.me` CNAME 추가
- [x] `https://api.jobradar.me/actuator/health` HTTP/2 200 확인
- [x] ALB HTTP 80 -> HTTPS 443 redirect 설정
- [x] 프론트 API base URL을 `https://api.jobradar.me`로 전환
- [x] 프론트 smoke test 완료
- [x] Backend GitHub Actions를 기존 JAR 배포에서 Docker + SSM 자동 배포로 전환
- [x] 자동 배포 성공 확인

ECR 첫 Push 결과:

```text
Git SHA Tag:
5b784b7951bc3dfadd8d679cf503252384863096

Manifest List:
sha256:46134d551c097783a41f1b1da0558b85d1e0c843df07df839f7fbf18a9d2fc3d

linux/amd64 Image Manifest:
sha256:6cffb923fa21c251ca24210ffc4db85b2d9bd4a38b0631251d08a4b81b548d43

BuildKit Provenance:
sha256:a5c832363b4be55fe0508857a94d3f83edc1fd37eceef88ee593f2b5eb686252
```

### 0.3 현재 운영 구조

```text
사용자
  -> 기존 AWS 계정 Route 53
  -> 기존 AWS 계정 CloudFront
  -> 기존 AWS 계정 S3 frontend

Frontend JavaScript
  -> https://api.jobradar.me
  -> 신규 AWS 계정 ALB
  -> 신규 AWS 계정 EC2 Docker backend
  -> 신규 AWS 계정 RDS MySQL
  -> 신규 AWS 계정 ElastiCache Serverless Valkey

GitHub Actions Backend
  -> OIDC AssumeRole
  -> Test/Build
  -> ECR Push
  -> SSM Run Command
  -> EC2 Docker container 교체
  -> /actuator/health 검증
```

### 0.4 다음 작업

다음 작업은 ALB Target Group 기반 Blue/Green 무중단 배포 자동화다.

채택한 방식:

```text
평상시:
  운영 EC2 1대만 유지

배포 시:
  새 EC2 1대 임시 생성
  새 EC2에 신규 Docker image 실행
  Target Group health check 통과 확인
  ALB listener를 새 Target Group으로 전환
  기존 EC2는 관찰 후 종료
```

남은 순서:

1. 기존 EC2와 RDS는 하루 정도 유지하며 롤백 가능성 확보
2. 예약 작업 분산 락과 작업 상태 관리 보강
3. Launch Template 생성
4. Blue/Green Target Group 2개 구성
5. GitHub Actions Role에 EC2/ELB Blue-Green 권한 추가
6. GitHub Actions에서 임시 EC2 생성, SSM Ready 대기, 컨테이너 실행 구현
7. Standby Target Group health check 대기 구현
8. ALB listener 전환 구현
9. 기존 Blue EC2 종료 보류/종료 정책 구현
10. 실패 시 이전 Target Group rollback 검증
11. 기존 AWS 계정 EC2/RDS 비용 리소스 정리

## 1. 목표

현재 운영 중인 JobRadar의 AWS 배포환경을 신규 AWS 계정으로 이관한다. 단순 이전이 아니라 운영 안정성, 보안, 배포 재현성을 함께 개선한다.

이번 이관에서 포함할 주요 변경 사항은 다음과 같다.

- 프론트엔드는 우선 기존 AWS 계정의 S3 + CloudFront를 유지
- Redis Docker 컨테이너를 ElastiCache Serverless Valkey로 이전
- 백엔드 Spring Boot를 Docker 컨테이너 기반 배포로 전환
- ECR을 백엔드 Docker 이미지 저장소로 사용
- ALB + EC2 Blue/Green 무중단 배포 도입
- GitHub Actions OIDC 기반 AWS 인증 도입
- SSM, Parameter Store, CloudWatch, Budget Alarm 등 운영 보강

## 2. 현재 운영 구조

이관 전 구조는 다음과 같았다.

```text
사용자
  -> CloudFront
  -> S3 정적 프론트엔드

프론트엔드
  -> https://jobradar.me/api/...
  -> EC2 Spring Boot
  -> RDS MySQL
  -> EC2 내부 Docker Redis
```

이관 전 GitHub Actions 배포 방식:

```text
Backend:
GitHub Actions
  -> Gradle test/build
  -> JAR를 EC2로 SCP 전송
  -> sudo systemctl restart jobradar

Frontend:
GitHub Actions
  -> npm ci && npm run build
  -> S3 sync
  -> CloudFront invalidation
```

이관 전 주요 설정:

- 백엔드 운영 DB: `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD`
- 백엔드 운영 Redis: `localhost:6379`
- 백엔드 포트: `8080`
- 프론트 API base URL: `https://jobradar.me`
- CORS 허용 origin: `http://localhost:5173`, `https://jobradar.me`
- 로컬 Docker Compose: MySQL 8.0, Valkey 8

### 2.1 현재 운영 구조

2026-06-27 기준 현재 운영 구조는 다음과 같다.

```text
사용자
  -> 기존 AWS 계정 Route 53
  -> 기존 AWS 계정 CloudFront
  -> 기존 AWS 계정 S3 정적 프론트엔드

프론트엔드
  -> https://api.jobradar.me
  -> 신규 AWS 계정 ALB
  -> 신규 AWS 계정 Backend EC2
  -> Backend Docker Container
  -> 신규 AWS 계정 RDS MySQL
  -> 신규 AWS 계정 ElastiCache Serverless Valkey

GitHub Actions Backend
  -> OIDC AssumeRole
  -> Test/Build
  -> ECR Push
  -> SSM Run Command
  -> Backend EC2 Docker container 교체
  -> /actuator/health 검증
```

## 3. 목표 운영 구조

최종 목표 구조는 다음과 같다.

```text
사용자
  -> Route 53
  -> CloudFront
  -> S3 정적 프론트엔드

프론트엔드
  -> https://api.jobradar.me
  -> ALB
  -> Active Target Group
  -> Active EC2
  -> Backend Docker Container
  -> RDS MySQL
  -> ElastiCache Serverless Valkey

GitHub Actions
  -> OIDC AssumeRole
  -> ECR Push
  -> 배포용 임시 EC2 생성
  -> SSM Run Command
  -> Standby Target Group health check
  -> ALB Target Group 전환
  -> 기존 EC2 종료
```

권장 도메인 구조:

```text
jobradar.me      -> CloudFront + S3 frontend
api.jobradar.me  -> ALB + backend EC2
```

이 구조는 프론트와 API 경로가 명확히 분리되어 CloudFront origin rule 복잡도를 줄이고, CORS 정책도 명확하게 관리할 수 있다.

## 4. 신규 AWS 계정 초기 설정

이관 작업 전 신규 AWS 계정에 다음 항목을 먼저 설정한다.

- Root 계정 MFA 활성화
- IAM 관리자 계정 분리
- AWS Budget 알림 설정
  - `$1`
  - `$5`
  - `$10`
- Cost Explorer 활성화
- 기본 리전 결정
  - 기존 구조와 동일하게 `ap-northeast-2` 권장
- 불필요한 리소스 생성 방지
  - NAT Gateway 사용 지양
  - ALB 외 추가 Load Balancer 생성 금지
  - RDS Multi-AZ 비활성화
  - 오래된 Snapshot 방치 금지

주의: 2025년 7월 15일 이후 신규 AWS Free Tier는 기존의 단순 12개월 무료 모델이 아니라 Free Plan/Paid Plan과 크레딧 구조가 섞여 있다. 이관 전에 AWS Free Tier, RDS, ElastiCache, ECR, ALB 가격 페이지를 기준으로 현재 조건을 재확인한다.

## 5. 네트워크 설계

### 5.1 VPC

신규 계정에 JobRadar 전용 VPC를 구성한다.

권장 구성:

- Public Subnet 2개
  - ALB
  - EC2
- Private Subnet 2개
  - RDS
  - ElastiCache
- Internet Gateway
- NAT Gateway는 비용 문제로 사용하지 않는 것을 기본값으로 한다.

Free Tier 비용을 강하게 줄여야 한다면 EC2를 Public Subnet에 두고, RDS/ElastiCache만 Private Subnet에 배치한다.

### 5.2 Security Group

권장 Security Group:

```text
sg-alb
  inbound:
    80  from 0.0.0.0/0
    443 from 0.0.0.0/0
  outbound:
    8080 to sg-backend

sg-backend
  inbound:
    8080 from sg-alb
  outbound:
    3306 to sg-rds
    6379 to sg-elasticache
    443  to internet

sg-rds
  inbound:
    3306 from sg-backend

sg-elasticache
  inbound:
    6379 from sg-backend
```

SSH 22번 포트는 열지 않는 것을 목표로 한다. 서버 접속과 배포 명령은 SSM Session Manager와 SSM Run Command를 사용한다.

## 6. 데이터 계층 이관

### 6.1 RDS MySQL

신규 계정에 RDS MySQL을 생성한다.

권장 설정:

- Engine: MySQL 8.x
- Instance class: Free Tier/저비용 범위의 micro 계열
- Multi-AZ: 비활성화
- Public access: 비활성화
- Storage auto scaling: 비용 상한을 고려해 신중히 설정
- Backup retention: 1-3일
- DB name: `job_radar`
- Character set/collation: 기존 DB와 동일하게 확인

실제 생성 정보:

```text
DB identifier: database-1
Engine: MySQL Community
Engine version: 8.4.8
Instance class: db.t4g.micro
AZ: ap-northeast-2a
Endpoint: database-1.c52m4gagoqlb.ap-northeast-2.rds.amazonaws.com
Port: 3306
DB name: job_radar
Public access: No
Security Group: jobradar-rds-sg
```

이관 방식:

```text
기존 RDS mysqldump
  -> SSH tunnel을 통해 로컬 PC에서 dump 생성
  -> 신규 RDS restore
  -> 앱 부팅 전 스키마 검증
```

이관 결과:

- `job_radar_dump.sql` 크기: 약 36MB
- 기존 RDS schema와 data를 신규 RDS에 import
- `ddl-auto: validate` 통과 확인

주의:

- 현재 운영 설정은 `ddl-auto: validate`이므로 신규 RDS에 스키마가 없으면 앱이 뜨지 않는다.
- Blue/Green 도입 후에는 DB 변경이 구버전/신버전 모두와 호환되어야 한다.
- 이후 운영 안정성을 위해 Flyway 도입을 검토한다.

### 6.2 ElastiCache for Valkey

현재 EC2 Docker Redis를 Redis 호환 프로토콜을 제공하는 ElastiCache Serverless Valkey로 이전했다.

Redis 사용처:

- Refresh Token 저장
- 로그아웃 처리
- Spring Cache
- Redisson 분산 락

실제 설정:

- Engine: Valkey
- Deployment option: Serverless
- Engine version: 9
- Encryption in transit: Always enabled
- Subnet group: Private Subnet
- Security Group: `sg-backend`에서만 6379 허용
- Endpoint: `jobradar-valkey-zn7sbh.serverless.apn2.cache.amazonaws.com:6379`
- Security Group: `jobradar-redis-sg`

주의:

- Serverless Valkey는 TLS 연결이 필요하다.
- 앱에서는 `REDIS_SSL=true`를 사용해 Redisson 주소를 `rediss://`로 구성한다.
- Spring Data Redis는 `spring.data.redis.ssl.enabled` 설정을 사용한다.

애플리케이션 설정 변경 방향:

현재:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

목표:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      ssl:
        enabled: ${REDIS_SSL:false}
```

Blue/Green에서 ElastiCache가 중요한 이유:

- Blue EC2와 Green EC2가 같은 refresh token 저장소를 공유한다.
- 전환 후에도 사용자가 강제 로그아웃될 가능성이 줄어든다.
- Redisson lock과 캐시가 서버 간 공유된다.

## 7. 백엔드 Docker 컨테이너화

현재 백엔드는 JAR를 EC2에 복사해 systemd로 실행한다. 이관 후에는 Docker 이미지 기반으로 배포한다.

목표:

```text
GitHub Actions
  -> Gradle test
  -> Docker image build
  -> ECR push
  -> EC2에서 ECR pull
  -> Docker container 실행
```

권장 이미지 태그:

```text
<account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/jobradar-backend:<git-sha>
```

ECR Repository가 Immutable이므로 가변 `latest`, `prod-current` 태그는 사용하지 않고 전체 Git SHA만 사용한다.

운영 EC2에는 다음만 설치한다.

- Docker
- AWS CLI
- SSM Agent
- CloudWatch Agent

운영 환경변수는 이미지에 포함하지 않는다. 컨테이너 실행 시 Parameter Store 또는 `/etc/jobradar/jobradar.env`를 통해 주입한다.

필수 환경변수:

```text
SPRING_PROFILES_ACTIVE=prod
DB_HOST=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
REDIS_HOST=
REDIS_PORT=6379
REDIS_SSL=true
VERTEX_PROJECT_ID=
VERTEX_LOCATION=
VERTEX_MODEL=
VERTEX_CREDENTIALS_PATH=
VERTEX_CREDENTIALS_JSON=
```

운영 Docker Compose 예시 방향:

```yaml
services:
  backend:
    image: ${BACKEND_IMAGE}
    container_name: jobradar-backend
    restart: unless-stopped
    ports:
      - "8080:8080"
    env_file:
      - /etc/jobradar/jobradar.env
```

운영 Compose에는 MySQL/Redis 컨테이너를 포함하지 않는다. RDS와 ElastiCache를 외부 관리형 서비스로 사용한다.

## 8. ECR 구성

백엔드 Docker 이미지는 Docker Hub 대신 ECR Private Repository에 저장한다.

이유:

- AWS IAM Role과 연동 가능
- GitHub Actions OIDC와 연동 가능
- EC2 IAM Role로 private image pull 가능
- 같은 리전 EC2 pull 데이터 전송 비용이 없다.
- Docker Hub 계정/token을 EC2에 저장하지 않아도 된다.

ECR Repository:

```text
jobradar-backend
```

Lifecycle Policy:

- untagged image는 1일 후 삭제
- Tagged 이미지 보관 개수 제한은 초기 이관 안정화 후 적용 여부를 결정한다.

비용 참고:

- 신규 ECR 고객은 private repository 저장소 월 500MB를 1년간 무료로 사용할 수 있다.
- Free Tier 이후 private repository 저장 비용은 작지만, 이미지가 계속 쌓이면 비용이 늘 수 있다.

## 9. GitHub Actions 개선

### 9.1 OIDC 인증

현재 프론트 배포는 장기 AWS Access Key를 사용한다. 신규 계정에서는 GitHub Actions OIDC를 사용한다.

목표:

```text
GitHub Actions
  -> AWS OIDC Provider
  -> IAM Role Assume
  -> ECR/S3/CloudFront/SSM/ALB 작업 수행
```

장점:

- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` 제거
- 장기 키 유출 리스크 감소
- IAM Role 단위로 권한 최소화 가능

### 9.2 Backend Workflow 현재 상태와 목표

```text
현재:
1. Gradle test/build
2. OIDC AssumeRole
3. 같은 Git SHA 이미지 존재 여부 확인
4. 없으면 linux/amd64 Docker image build
5. ECR login 및 Git SHA image push
6. 있으면 build/push 생략
7. SSM Run Command로 운영 EC2에 배포 명령 실행
8. EC2에서 Parameter Store 기반 app.env 재생성
9. Vertex credentials JSON 파일 복원
10. 새 Docker image pull
11. 기존 container 교체
12. /actuator/health 확인

다음 목표:
13. 배포용 임시 EC2 생성
14. Standby Target Group에 새 EC2 등록
15. Target Group health check 확인
16. ALB listener 전환
17. 관찰 및 예약 작업 상태 확인
18. 기존 Blue EC2 종료
```

기존 JAR 기반 EC2 배포 job은 제거됐다. 기존 EC2를 중지해도 백엔드 GitHub Actions 배포가 실패하지 않는다.

### 9.3 Frontend Workflow 목표

```text
1. checkout
2. setup Node
3. npm ci
4. npm run build
5. S3 sync
6. CloudFront invalidation
```

프론트 환경변수:

```text
VITE_API_BASE_URL=https://api.jobradar.me
```

프론트는 기존 AWS 계정의 S3 + CloudFront에 계속 배포한다. GitHub Actions 프론트 배포는 `VITE_API_BASE_URL=https://api.jobradar.me`로 build 후 S3 sync와 CloudFront invalidation을 수행한다.

## 10. Blue/Green 무중단 배포 전략

### 10.1 기본 개념

Blue/Green은 고정된 서버 이름이 아니라 역할이다.

```text
Blue  = 현재 운영 중인 안정 버전
Green = 이번에 배포할 새 버전
```

배포가 성공하면 Green이 운영 서버가 되고, 다음 배포에서는 이 서버가 Blue 역할을 한다.

### 10.2 목표 흐름

```text
현재 상태:
ALB -> Blue Target Group -> Blue EC2

배포:
1. GitHub Actions가 새 Docker image를 ECR에 push
2. Green EC2 생성
3. Green EC2가 ECR에서 새 image pull
4. Green container 실행
5. /actuator/health 확인
6. Green Target Group에 Green EC2 등록
7. ALB listener를 Green Target Group으로 전환
8. 일정 시간 모니터링
9. 실행 중인 예약 작업이 없는지 확인
10. 기존 Blue EC2 종료
```

### 10.3 Green EC2 생성 방식

Green EC2는 Launch Template으로 생성한다. 채택한 방식은 평상시 EC2 1대만 유지하고, 배포 중에만 새 EC2를 임시로 추가 생성하는 구조다.

채택 이유:

- 단일 EC2 container 교체 방식보다 무중단 성격이 강하다.
- EC2 2대를 상시 유지하지 않아 비용을 줄일 수 있다.
- 새 EC2에서 Docker image, SSM, Parameter Store, RDS, Valkey 연결을 검증한 뒤 ALB를 전환할 수 있다.
- 실패 시 기존 운영 EC2와 기존 Target Group은 건드리지 않으므로 rollback이 단순하다.

Launch Template에 포함할 항목:

- AMI
- Instance type
- IAM Instance Profile
- Security Group
- Subnet
- User Data
- EBS size
- Metadata options

User Data 또는 SSM Run Command가 수행할 작업:

```text
1. ECR login
2. Docker image pull
3. env file 준비
4. backend container 실행
5. health check 대기
```

GitHub Actions Blue/Green 배포 흐름:

```text
1. ECR image push
2. 현재 ALB HTTPS listener가 바라보는 active Target Group 조회
3. standby Target Group 결정
4. Launch Template으로 새 EC2 생성
5. 새 EC2 running + SSM ready 대기
6. SSM Run Command로 새 EC2에 container 실행
7. standby Target Group에 새 EC2 등록
8. standby Target Group health check healthy 대기
9. ALB listener를 standby Target Group으로 전환
10. 짧은 관찰 시간 동안 health/API 확인
11. 기존 active EC2 deregister
12. 기존 active EC2 terminate 또는 stop
```

배포 중 비용:

```text
평상시: Backend EC2 1대
배포 중: 기존 EC2 1대 + 신규 EC2 1대
배포 완료 후: 신규 EC2 1대
```

### 10.4 구버전 EC2 처리

배포 성공 직후 기존 Blue EC2를 바로 삭제하지 않고 짧은 관찰 시간을 둔다.

권장:

```text
ALB 전환
  -> 5-15분 모니터링
  -> 5xx, health, 주요 API 확인
  -> 기존 Blue의 예약 작업 실행 상태 확인
  -> 문제 없으면 기존 Blue EC2 종료
```

Rollback 전략:

- 전환 직후 문제 발생: ALB listener를 이전 Blue Target Group으로 되돌린다.
- 기존 Blue 종료 후 문제 발생: 이전 ECR image tag로 새 EC2를 생성한다.
- 기존 Blue에서 예약 작업이 실행 중이면 작업 완료까지 종료를 보류한다.

### 10.5 세션/인증 상태

JWT access token은 클라이언트가 들고 있으므로 서버 메모리 세션 의존도는 낮다. 다만 refresh token은 Redis에 저장되므로, Blue/Green 환경에서는 Redis가 EC2 로컬이면 안 된다.

ElastiCache로 이전하면:

- Blue와 Green이 같은 refresh token 저장소를 본다.
- 배포 전환 후 재로그인 발생 가능성이 줄어든다.
- Redisson lock과 cache도 공유된다.

### 10.6 예약 작업 운영

예약 작업은 별도 Lambda, EventBridge 또는 ECS 작업으로 분리하지 않고 Spring Boot 내부 `@Scheduled` 방식으로 유지한다.

현재 예약 작업:

```text
매일 00:00         만료 공고 종료
매일 03:00         채용공고 크롤링
매주 월요일 03:00  상시채용 공고 검사
```

Blue/Green 배포 중에는 두 애플리케이션이 잠시 함께 실행되므로 ElastiCache 기반 Redisson 분산 락으로 각 작업이 한 번만 실행되도록 한다.

권장 락 키:

```text
scheduler:close-expired-jobs
scheduler:daily-crawling
scheduler:always-open-check
```

운영 원칙:

- 각 예약 작업은 서로 다른 락 키를 사용한다.
- 락은 스케줄 메서드 호출이 아니라 실제 작업이 끝날 때까지 유지한다.
- 상시채용 검사의 비동기 실행은 실제 비동기 작업 내부에서 락과 상태를 관리하거나, 예약 실행 경로에서는 동기 완료를 기다리도록 조정한다.
- 공유 Redis에 작업별 `RUNNING`, `SUCCESS`, `FAILED` 상태와 실행 인스턴스, 시작/종료 시각을 기록한다.
- 동일 작업이 이미 `RUNNING`이면 다른 인스턴스는 실행하지 않고 로그만 남긴다.
- 예외가 발생해도 락이 해제되고 작업 상태가 `FAILED`로 기록되도록 한다.
- 관리자 수동 실행도 예약 실행과 같은 락을 사용해 중복 크롤링을 방지한다.
- 배포 파이프라인은 기존 Blue에 `RUNNING` 작업이 있으면 EC2 종료를 보류하고, 완료 후 종료한다.
- 장시간 멈춘 작업을 식별할 수 있도록 작업별 최대 예상 실행 시간을 기준으로 경고를 남긴다.

ALB가 Green으로 전환된 뒤에도 기존 Blue에서 시작한 예약 작업은 완료될 때까지 실행될 수 있다. Blue와 Green은 같은 RDS와 ElastiCache를 사용하므로 DB 변경은 양쪽 버전과 호환되어야 한다.

## 11. 운영 비밀값 관리

### 11.1 Parameter Store

비용을 줄이기 위해 우선 SSM Parameter Store Standard를 사용한다.

현재 사용 중인 parameter:

```text
/jobradar/prod/DB_HOST
/jobradar/prod/DB_USERNAME
/jobradar/prod/DB_PASSWORD
/jobradar/prod/REDIS_HOST
/jobradar/prod/REDIS_PORT
/jobradar/prod/REDIS_SSL
/jobradar/prod/CORS_ALLOWED_ORIGINS
/jobradar/prod/JWT_SECRET
/jobradar/prod/VERTEX_PROJECT_ID
/jobradar/prod/VERTEX_LOCATION
/jobradar/prod/VERTEX_MODEL
/jobradar/prod/VERTEX_CREDENTIALS_PATH
/jobradar/prod/VERTEX_CREDENTIALS_JSON
```

민감값은 SecureString을 사용한다. `VERTEX_CREDENTIALS_JSON`은 서비스 계정 JSON 전체를 SecureString으로 저장하고, 배포 시 EC2에서 `/opt/jobradar/secrets/vertex-credentials.json` 파일로 렌더링한다.

### 11.2 Vertex AI Credential

현재 Vertex AI는 서비스 계정 JSON 파일 경로 기반이다. 신규 EC2에서는 SSM Parameter Store SecureString에 저장한 JSON을 배포 스크립트에서 파일로 렌더링한다.

현재 경로:

```text
Host path: /opt/jobradar/secrets/vertex-credentials.json
Container path: /app/secrets/vertex-credentials.json
Docker mount: -v /opt/jobradar/secrets:/app/secrets:ro
VERTEX_CREDENTIALS_PATH=/app/secrets/vertex-credentials.json
```

서비스 계정 JSON은 Docker 이미지에 포함하지 않는다.

## 12. 관측성과 알림

### 12.1 CloudWatch Logs

다음 로그를 CloudWatch Logs로 전송한다.

- backend container stdout/stderr
- deploy script log
- system log
- ALB access log는 비용을 고려해 선택

보존 기간:

- 초기: 3일 또는 7일
- 장애 분석 필요 시 일시적으로 연장

### 12.2 CloudWatch Alarm

권장 알림:

- ALB 5xx 증가
- Target Group unhealthy host 발생
- EC2 CPU 과다
- EC2 memory/disk 사용률 과다
- RDS CPU 과다
- RDS connection 과다
- ElastiCache memory 사용률 과다
- ElastiCache evictions 발생

### 12.3 Budget Alarm

신규 계정에는 반드시 Budget Alarm을 설정한다.

권장:

- `$1`
- `$5`
- `$10`

이관 테스트 중 ALB, ElastiCache, RDS snapshot, public IPv4 비용이 예상보다 커질 수 있다.

## 13. 보안 개선

이번 이관에서 함께 적용할 보안 개선 항목:

- GitHub Actions OIDC 적용
- AWS 장기 Access Key 제거
- EC2 SSH 22번 포트 차단
- SSM Session Manager 사용
- RDS public access 비활성화
- ElastiCache private subnet 배치
- Security Group source를 SG 단위로 제한
- Swagger 운영 접근 제한
- CloudFront Response Headers Policy 적용

CloudFront 보안 헤더 권장:

```text
Strict-Transport-Security
X-Content-Type-Options
X-Frame-Options
Referrer-Policy
Content-Security-Policy
```

JWT를 localStorage에 저장하고 있으므로 XSS 방어가 중요하다. CSP는 처음부터 강하게 걸기보다 report-only 또는 느슨한 정책으로 시작해 점진 강화한다.

## 14. 애플리케이션 변경 필요 항목

이관을 위해 코드 또는 설정 변경이 필요한 항목이다.

### 14.1 Redis 설정 외부화

현재 운영 Redis host는 `localhost`로 고정되어 있다.

변경 필요:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
```

### 14.2 CORS 설정 외부화

현재 CORS origin이 코드에 하드코딩되어 있다.

변경 권장:

```text
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS}
```

운영 값:

```text
https://jobradar.me
```

API 도메인을 `api.jobradar.me`로 분리해도 CORS origin은 프론트 origin인 `https://jobradar.me`가 된다.

### 14.3 Frontend API URL 변경

현재:

```text
VITE_API_BASE_URL=https://jobradar.me
```

목표:

```text
VITE_API_BASE_URL=https://api.jobradar.me
```

### 14.4 Dockerfile 추가

백엔드 Docker 이미지를 빌드하기 위한 Dockerfile이 필요하다.

권장:

- multi-stage build 또는 bootJar 산출물 복사 방식
- Java 21 runtime 이미지 사용
- non-root user 실행
- JVM memory option 설정

### 14.5 Actuator health 노출 범위 조정

ALB health check를 위해 `/actuator/health`는 접근 가능해야 한다. 다만 상세 정보는 외부에 노출하지 않는다.

권장:

```text
management.endpoint.health.show-details=never
```

## 15. 데이터 마이그레이션 고려사항

### 15.1 RDS 데이터

이관 전:

- 기존 RDS snapshot 생성
- `mysqldump` 백업
- row count 검증 쿼리 준비

이관 후:

- 신규 RDS restore
- 주요 테이블 row count 비교
- 로그인/회원/스크랩/공고 조회 확인
- 크롤러 실행 전 중복 저장 로직 확인

### 15.2 Redis 데이터

현재 Redis에는 refresh token, cache, lock 정보가 있다.

마이그레이션 선택:

- refresh token을 이전하지 않는다.
  - 단순하지만 사용자 재로그인 발생 가능
- Redis dump를 ElastiCache로 이전한다.
  - 복잡도 증가

JobRadar의 현재 규모에서는 Redis 데이터는 이전하지 않고, 배포 전환 후 사용자의 refresh token 재발급 실패 가능성을 감수하는 방식도 현실적이다. 다만 사용자 경험을 중요하게 보면 이관 직전 로그인 세션 영향을 공지하거나 refresh token 저장소 이전을 별도 검토한다.

## 16. 비용 관리 포인트

주의해야 할 비용 항목:

- ALB hourly cost
- ElastiCache node cost
- RDS running cost
- RDS storage/snapshot
- EC2 public IPv4
- EBS volume
- CloudWatch Logs 저장량
- NAT Gateway
- 오래된 ECR image
- CloudFront invalidation 과다 사용

비용 절감 원칙:

- NAT Gateway는 사용하지 않는다.
- ElastiCache는 초기 단일 노드로 시작한다.
- RDS Multi-AZ는 초기 비활성화한다.
- ECR lifecycle policy를 적용한다.
- CloudWatch Logs retention을 짧게 둔다.
- 배포 성공 후 기존 Blue EC2는 반드시 종료한다.
- 수동 snapshot은 목적이 끝나면 삭제한다.

## 17. 검증 체크리스트

### 17.1 인프라 검증

- Route 53 DNS 정상
- ACM 인증서 정상
- CloudFront HTTPS 정상
- ALB HTTPS 정상
- Target Group health check 정상
- RDS private 접근 정상
- ElastiCache private 접근 정상
- SSM Session Manager 접속 정상
- GitHub Actions OIDC assume role 정상

### 17.2 백엔드 검증

- Docker image build 성공
- ECR push 성공
- EC2 ECR pull 성공
- Container boot 성공
- `/actuator/health` 정상
- DB 연결 정상
- Redis 연결 정상
- JWT 로그인/재발급 정상
- 세 예약 작업 단독 실행 정상
- Blue/Green 동시 실행 시 예약 작업 중복 방지
- 관리자 수동 크롤링과 예약 크롤링 중복 방지
- 예약 작업 성공/실패 상태 기록 정상
- Swagger 운영 노출 정책 확인

### 17.3 프론트엔드 검증

- S3 sync 정상
- CloudFront invalidation 정상
- `jobradar.me` 접속 정상
- API base URL이 `api.jobradar.me`로 동작
- CORS 오류 없음
- 로그인/회원가입/공고조회/스크랩 정상

### 17.4 Blue/Green 검증

- Green EC2 생성 정상
- Green container health check 정상
- Green Target Group 등록 정상
- ALB listener 전환 정상
- 기존 Blue Target Group 제거 정상
- 예약 작업 실행 중 Blue EC2 종료 보류
- 예약 작업 완료 후 Blue EC2 종료 진행
- 기존 Blue EC2 종료 정상
- 실패 시 이전 Target Group rollback 가능

## 18. 권장 적용 순서

1. [x] 신규 AWS 계정 기본 보안과 비용 알림 설정
2. [x] VPC, Subnet, Route Table, Security Group 구성
3. [x] RDS와 Valkey Private Subnet Group 구성
4. [x] ECR과 Lifecycle Policy 구성
5. [x] 백엔드 Dockerfile과 로컬 통합 검증 환경 구성
6. [x] GitHub Actions OIDC와 ECR Push 구성
7. [x] `linux/amd64` Git SHA 이미지 Build/Push 검증
8. [x] RDS MySQL 생성
9. [x] ElastiCache Serverless Valkey 생성
10. [x] Parameter Store와 Vertex 인증정보 구성
11. [ ] 예약 작업 분산 락과 상태 관리 구현
12. [x] EC2 Instance Role 구성
13. [ ] Launch Template 구성
14. [x] ALB, Target Group, ACM 구성
15. [x] Green EC2 컨테이너 실행과 실제 RDS·Valkey 연결 검증
16. [x] RDS 데이터 이관
17. [x] Frontend API URL, CORS, DNS 전환
18. [x] GitHub Actions Docker + SSM 자동 배포 구성
19. [ ] GitHub Actions Blue/Green 자동 배포 확장
20. [ ] CloudWatch와 Rollback 검증
21. [ ] 기존 AWS 계정 EC2/RDS 정리

## 19. 주요 리스크

### 19.1 DB 변경 리스크

Blue와 Green이 같은 RDS를 사용하므로 DB 스키마 변경은 반드시 backward-compatible 해야 한다.

위험한 변경:

- 컬럼 삭제
- 컬럼명 변경
- 타입 변경
- enum 의미 변경
- 구버전 코드가 읽지 못하는 구조 변경

권장 방식:

```text
1. 기존 코드와 호환되는 컬럼 추가
2. 새 코드 배포
3. 데이터 백필
4. 충분히 안정화 후 미사용 컬럼 제거
```

### 19.2 Valkey 이전 리스크

EC2 로컬 Redis를 ElastiCache for Valkey로 변경하면 refresh token 저장소가 바뀐다. 기존 Redis 데이터를 이전하지 않으면 일부 사용자는 재로그인이 필요할 수 있다.

### 19.3 예약 작업 리스크

Blue와 Green이 동시에 실행되면 같은 `@Scheduled` 작업이 중복 실행될 수 있다. 특히 상시채용 검사는 비동기 실행이므로 스케줄 메서드 반환만으로 실제 완료를 판단하면 안 된다.

대응:

- ElastiCache 기반 작업별 분산 락 적용
- 실제 작업 실행 구간 전체에 락 유지
- 작업 상태와 실행 인스턴스 기록
- 관리자 수동 실행에도 동일 락 적용
- 실행 중 작업이 있는 Blue EC2 종료 보류
- 비정상 종료로 남은 `RUNNING` 상태를 감지하는 시간 초과 경고 적용

### 19.4 비용 리스크

Free Tier 계정이라도 ALB, ElastiCache, RDS, public IPv4, snapshot, CloudWatch Logs에서 비용이 발생할 수 있다. Budget Alarm 없이 작업하지 않는다.

### 19.5 배포 권한 리스크

OIDC Role 권한을 너무 넓게 주면 보안상 위험하다. ECR, S3, CloudFront, SSM, EC2, ELB 권한을 필요한 리소스 범위로 제한한다.

## 20. 최종 목표 상태

이관 완료 후 JobRadar의 운영 상태는 다음을 만족해야 한다.

- 신규 AWS 계정에서 운영
- 프론트엔드는 S3 + CloudFront로 배포
- 백엔드는 Docker image 기반으로 ECR에서 배포
- EC2는 직접 SSH 없이 SSM으로 관리
- Redis 호환 저장소는 ElastiCache for Valkey로 분리
- RDS는 private access만 허용
- ALB 기반 Blue/Green 배포 가능
- 배포 실패 시 rollback 가능
- 예약 작업은 Spring Boot 내부 스케줄러로 실행
- Blue/Green 공존 중에도 예약 작업은 한 번만 실행
- 실행 중 예약 작업이 있는 EC2는 작업 완료 전 종료하지 않음
- 운영 비밀값은 Parameter Store 중심으로 관리
- CloudWatch와 Budget Alarm으로 장애와 비용을 감시
- 기존 AWS 계정의 과금 리소스는 정리 가능

## 21. 월 예상 비용

아래 비용은 `ap-northeast-2` 리전, 월 730시간, 낮은 트래픽, 단일 운영 EC2, 배포 시에만 Green EC2를 잠깐 생성하는 것을 기준으로 한 보수적 추정이다. 실제 비용은 인스턴스 타입, 스토리지 크기, 로그량, 트래픽, AWS Free Tier 크레딧 적용 여부에 따라 달라진다.

### 21.1 산정 가정

- Region: `ap-northeast-2`
- Backend EC2: `t3.micro` 또는 동급 micro 1대 상시 운영
- Green EC2: 배포 시에만 생성, 월 4시간 이내 사용
- ALB: 1개 상시 운영, 2개 AZ 사용
- RDS: MySQL micro 계열 1대, Single-AZ
- ElastiCache: Valkey `cache.t4g.micro` 단일 노드
- S3: 프론트엔드 정적 파일 1GB 미만
- CloudFront: 월 100GB 이하 전송
- ECR: 백엔드 이미지 10개 이하 보관
- NAT Gateway: 사용하지 않음
- CloudWatch Logs: 월 5GB 이하 수집, 보존기간 3-7일
- Route 53: Public Hosted Zone 1개

### 21.2 서비스별 예상 비용

| 서비스 | 용도 | 월 예상 |
|---|---|---:|
| EC2 | Blue backend 1대 상시 운영 | 약 `$8-11` |
| EC2 Green | 배포 시 임시 EC2 | 약 `$0-1` |
| EBS | EC2 root volume 20GB 내외 | 약 `$2` |
| Public IPv4 | ALB가 2개 AZ에서 사용하는 public IPv4 2개 + EC2 1개 가정 | 약 `$10.95` |
| ALB | Blue/Green 트래픽 전환 | 약 `$18-25` |
| RDS MySQL | 운영 DB micro, Single-AZ | 약 `$18-23` |
| RDS Storage/Backup | 20GB 내외, 짧은 백업 보존 | 약 `$2-4` |
| ElastiCache | Valkey micro 단일 노드 | 약 `$14-15` |
| S3 | 프론트 정적 파일 저장/요청 | 약 `$0-1` |
| CloudFront | 낮은 트래픽 CDN | 약 `$0-2` |
| ECR | 백엔드 Docker 이미지 저장 | 약 `$0-1` |
| Route 53 | Hosted Zone 1개 | 약 `$0.50` |
| ACM | Public TLS 인증서 | `$0` |
| SSM | Session Manager, Run Command, Standard Parameter | 대체로 `$0` |
| CloudWatch | 기본 메트릭, 로그 소량, 알람 일부 | 약 `$0-3` |
| Data Transfer | 100GB 이하 가정 | 약 `$0` |

### 21.3 합산 예상

Free Tier 크레딧이나 무료 사용량을 적용하지 않은 온디맨드 기준:

```text
낮은 추정: 약 $72/month
보수적 추정: 약 $80/month
높은 추정: 약 $90/month
```

즉, 이번 migration plan을 모두 적용하면 월 비용은 대략 `$75-85` 수준으로 보는 것이 현실적이다.

ElastiCache는 Valkey 기준으로 산정한다. AWS Price List API 기준 `ap-northeast-2`의 Valkey micro 단가는 다음과 같다.

```text
cache.t4g.micro: $0.0192/hour -> 약 $14.02/month
cache.t3.micro:  $0.0200/hour -> 약 $14.60/month
```

따라서 가능하면 `cache.t4g.micro`를 우선 검토하고, 호환성이나 계정 선택지에 따라 `cache.t3.micro`를 대안으로 둔다.

현재 비용이 월 `$30` 수준이라면, 비용 증가의 주된 원인은 다음이다.

- ALB 추가
- ElastiCache 추가
- Public IPv4 비용
- Blue/Green을 위한 운영 복잡도 증가

### 21.4 Free Tier 계정에서의 체감 비용

2025년 7월 15일 이후 신규 AWS Free Tier는 서비스별 12개월 무료 한도와 별개로 Free Plan/Paid Plan 및 최대 `$200` 크레딧 구조가 적용될 수 있다. 따라서 실제 카드 청구액은 초기에 `$0`일 수 있지만, 위 구조는 매월 약 `$75-85`의 크레딧을 소모하는 구조로 보는 것이 안전하다.

예시:

```text
$200 credit / $80 per month = 약 2.5개월
```

따라서 Free Tier 계정으로 이전하더라도 이 구조를 장기간 유지하면 크레딧 소진 후 월 `$75-85` 수준의 비용이 발생할 수 있다.

### 21.5 비용 절감 대안

비용을 낮추려면 다음 순서로 조정한다.

1. ALB Blue/Green 대신 EC2 1대 + Nginx 8080/8081 포트 스위칭을 사용한다.
   - ALB 비용과 ALB public IPv4 비용을 줄일 수 있다.
2. ElastiCache 대신 로컬 Redis를 유지한다.
   - 무중단 전환 시 refresh token 공유 문제가 생길 수 있다.
3. EC2에 public IPv4를 붙이지 않고 SSM + VPC Endpoint 또는 NAT 없이 가능한 구성을 검토한다.
   - 단, VPC Endpoint도 서비스별 시간 비용이 있으므로 신중히 비교한다.
4. CloudWatch Logs 보존기간을 3일로 제한한다.
5. ECR lifecycle policy로 최근 이미지 5-10개만 유지한다.
6. RDS 백업 보존기간과 수동 snapshot을 최소화한다.

### 21.6 비용 판단 결론

이번 목표 구조는 운영 안정성과 포트폴리오 완성도는 크게 올라가지만, Free Tier 이후 비용은 기존보다 늘어난다. 비용 최우선이면 `EC2 1대 + Docker + ElastiCache 또는 로컬 Redis + Nginx 포트 스위칭`이 낫고, 운영 고도화 학습과 무중단 배포 경험이 목표라면 `ALB + Blue/Green + ElastiCache + ECR` 구조가 더 적합하다.
