# Payment API

A RESTful payment service built with Spring Boot, providing wallet management, P2P transfers, transaction history, webhook notifications, and JWT authentication.

## Tech Stack

| Technology | Version | Description |
|---|---|---|
| Java | 17 | Programming Language |
| Spring Boot | 3.2.1 | Application Framework |
| Spring Security | 6.x | Authentication & Authorization |
| Spring Data JPA | 3.2.x | Data Access Layer |
| PostgreSQL | 16 | Relational Database |
| JJWT | 0.12.6 | JWT Token Library |
| Lombok | latest | Boilerplate Code Reduction |
| Docker | latest | Containerization |

## Features

- User registration and authentication (JWT)
- Wallet management (create, deposit, withdraw)
- P2P transfers between wallets
- Transaction history with pagination and type filtering
- Transaction refunds
- Webhook notifications for transaction events
- Idempotency key support for safe retries
- Global exception handling

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 16 (or Docker)

### Run with Docker

```bash
# Clone the repository
git clone https://github.com/daoninhthai/payment-api.git
cd payment-api

# Copy environment variables
cp .env.example .env

# Start services
docker-compose up -d
```

The API will be available at `http://localhost:8080`.

### Run Locally

```bash
# Start PostgreSQL
# Make sure PostgreSQL is running on localhost:5432

# Create database
createdb payment_db

# Build and run
mvn clean install
mvn spring-boot:run
```

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and get JWT token |

### Wallets

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/wallets/{id}` | Get wallet information |
| POST | `/api/wallets/{id}/deposit` | Deposit money into wallet |
| POST | `/api/wallets/{id}/withdraw` | Withdraw money from wallet |
| POST | `/api/wallets/{id}/transfer` | Transfer money to another wallet |
| GET | `/api/wallets/{id}/transactions` | Get transaction history |
| POST | `/api/wallets/transactions/{id}/refund` | Refund a transaction |

### Webhooks

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/webhooks` | Register a webhook |
| GET | `/api/webhooks` | List webhooks |
| DELETE | `/api/webhooks/{id}` | Deactivate a webhook |

## API Usage Examples

### Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "secret123",
    "fullName": "Nguyen Van A"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "secret123"
  }'
```

### Deposit

```bash
curl -X POST http://localhost:8080/api/wallets/1/deposit \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: unique-key-123" \
  -d '{
    "amount": 1000000,
    "description": "Initial deposit"
  }'
```

### Transfer

```bash
curl -X POST http://localhost:8080/api/wallets/1/transfer \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "toWalletId": 2,
    "amount": 500000,
    "description": "Payment for services"
  }'
```

## Project Structure

```
src/main/java/com/daoninhthai/payment/
├── PaymentApiApplication.java
├── annotation/
│   └── Idempotent.java
├── config/
│   ├── IdempotencyInterceptor.java
│   └── SecurityConfig.java
├── controller/
│   ├── AuthController.java
│   ├── WalletController.java
│   └── WebhookController.java
├── dto/
│   ├── request/
│   │   ├── DepositRequest.java
│   │   ├── LoginRequest.java
│   │   ├── RefundRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── TransferRequest.java
│   │   ├── WebhookRequest.java
│   │   └── WithdrawRequest.java
│   └── response/
│       ├── ApiError.java
│       ├── AuthResponse.java
│       ├── PageResponse.java
│       ├── TransactionResponse.java
│       └── WalletResponse.java
├── entity/
│   ├── Transaction.java
│   ├── User.java
│   ├── Wallet.java
│   ├── Webhook.java
│   ├── WebhookEvent.java
│   └── enums/
│       ├── TransactionStatus.java
│       └── TransactionType.java
├── exception/
│   ├── BadRequestException.java
│   ├── GlobalExceptionHandler.java
│   ├── InsufficientBalanceException.java
│   └── ResourceNotFoundException.java
├── repository/
│   ├── TransactionRepository.java
│   ├── UserRepository.java
│   ├── WalletRepository.java
│   ├── WebhookEventRepository.java
│   └── WebhookRepository.java
├── security/
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java
│   └── JwtTokenProvider.java
└── service/
    ├── AuthService.java
    ├── IdempotencyService.java
    ├── TransactionService.java
    ├── WalletService.java
    └── WebhookService.java
```

## Author

**Dao Ninh Thai** - [thaimeo1131@gmail.com](mailto:thaimeo1131@gmail.com)

## License

This project is licensed under the MIT License.
