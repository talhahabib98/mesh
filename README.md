# Mesh

Mesh is a distributed real-time chat application. Any number of stateless application
instances can sit behind a load balancer; a client connected to one instance still sees
messages sent through another, because delivery is fanned out through Redis pub/sub
rather than kept in each instance's in-memory WebSocket broker.

## Why it's "distributed"

A naive Spring WebSocket/STOMP setup only delivers messages to clients connected to the
*same* JVM, because `SimpleBroker` is in-memory. Mesh breaks that constraint:

1. A client sends a message over STOMP to `/app/chat.send`.
2. The receiving instance persists it to Postgres, then **publishes** it to a Redis
   channel instead of broadcasting it directly.
3. Every instance (including the one that received the message) is subscribed to that
   channel. Each instance's subscriber delivers the message to its own
   locally-connected WebSocket sessions via `SimpMessagingTemplate`.

So it doesn't matter which instance a client's WebSocket is pinned to — delivery is
uniform across the fleet. The same pattern is used for presence (join/leave) events.
See [`ChatMessagePublisher`](src/main/java/com/mesh/realtime/ChatMessagePublisher.java),
[`ChatMessageSubscriber`](src/main/java/com/mesh/realtime/ChatMessageSubscriber.java),
and [`PresenceWebSocketEventListener`](src/main/java/com/mesh/chat/PresenceWebSocketEventListener.java).

```
 client A ──WS──▶ instance 1 ──persist──▶ Postgres
                        │
                        ▼
                  Redis pub/sub
                   (chat channel)
                   │          │
        ┌──────────┘          └──────────┐
        ▼                                ▼
  instance 1 (local WS)            instance 2 (local WS)
        │                                │
        ▼                                ▼
   client A sees it                client B sees it
                                   (never touched instance 1)
```

## Tech stack

| Concern              | Choice                                                  |
|-----------------------|----------------------------------------------------------|
| Language / runtime    | Java 21, Spring Boot 4                                   |
| Real-time transport   | Spring WebSocket + STOMP (SockJS fallback for browsers)  |
| Cross-instance fanout  | Redis pub/sub (also backs presence, via Redis sets)       |
| Persistence           | PostgreSQL, Spring Data JPA, Flyway migrations            |
| Auth                  | Spring Security + stateless JWT (jjwt)                    |
| Build                 | Maven (wrapper committed, no local install required)      |
| Tests                 | JUnit 5, Mockito, AssertJ, Testcontainers (Postgres+Redis) |
| Local orchestration   | Docker Compose: Postgres, Redis, 2 app instances, Nginx LB |
| Frontend (demo)       | Plain HTML/CSS/JS, SockJS + `@stomp/stompjs` from a CDN   |

## Running locally

### Option A: Docker Compose (closest to production topology)

```bash
docker compose up --build
```

This starts Postgres, Redis, two app instances (`node-1`, `node-2`), and an Nginx
load balancer on `http://localhost:8080`. Requests are round-robined across both
instances — open two browser tabs and watch messages sync regardless of which
instance each tab lands on.

### Option B: Single instance, for quick iteration

```bash
docker run -d -p 5432:5432 -e POSTGRES_DB=mesh -e POSTGRES_USER=mesh -e POSTGRES_PASSWORD=mesh postgres:17-alpine
docker run -d -p 6379:6379 redis:7-alpine
./mvnw spring-boot:run
```

Then open `http://localhost:8080`.

### Running two local instances without Docker

```bash
./mvnw -DskipTests package
java -jar target/mesh-0.0.1-SNAPSHOT.jar --server.port=8081 --mesh.node.id=node-a
java -jar target/mesh-0.0.1-SNAPSHOT.jar --server.port=8082 --mesh.node.id=node-b
```

Register/open a room on `:8081` and `:8082` in separate tabs — both share the same
Postgres/Redis, so messages sent on one are delivered on the other.

## Tests

```bash
./mvnw test
```

Integration tests spin up real Postgres and Redis containers via Testcontainers
(Docker must be running). [`ChatBroadcastDistributionTest`](src/test/java/com/mesh/realtime/ChatBroadcastDistributionTest.java)
specifically simulates two separate app instances subscribed to the same Redis channel
and asserts that a single publish reaches both — the core guarantee this whole project
is built around.

## API overview

| Method | Path                          | Auth | Description                      |
|--------|--------------------------------|------|-----------------------------------|
| POST   | `/api/auth/register`           | No   | Create an account, returns a JWT |
| POST   | `/api/auth/login`               | No   | Returns a JWT                    |
| POST   | `/api/rooms`                    | Yes  | Create a room                    |
| GET    | `/api/rooms`                    | Yes  | List rooms                       |
| GET    | `/api/rooms/{roomId}/messages`  | Yes  | Message history (paginated by `limit`) |
| GET    | `/api/rooms/{roomId}/presence`  | Yes  | Online usernames in a room       |
| STOMP  | CONNECT with `Authorization: Bearer <jwt>` header on `/ws` | - | WebSocket handshake |
| STOMP  | SEND `/app/chat.send` `{roomId, content}` | - | Send a chat message |
| STOMP  | SUBSCRIBE `/topic/rooms/{roomId}` | - | Receive chat messages |
| STOMP  | SUBSCRIBE `/topic/rooms/{roomId}/presence` | - | Receive join/leave events |

## Project structure

```
com.mesh
├── auth        JWT issuing/validation, Spring Security wiring, register/login
├── user        User entity + repository
├── room        Chat room entity, service, REST controller
├── message     Persisted chat message entity, service, history endpoint
├── chat        WebSocket/STOMP config, the chat + presence message handlers
└── realtime    Redis pub/sub publishers/subscribers, presence tracking - the
               cross-instance distribution layer
```
