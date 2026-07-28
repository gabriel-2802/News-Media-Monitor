# Manager Service — API & Messaging Reference

Base path: all HTTP endpoints are mounted under the servlet context path `/manager` (see `application.yaml`), so a route documented here as `/api/auth/login` is actually served at `/manager/api/auth/login`.

Auth model: stateless JWT bearer tokens (`Authorization: Bearer <token>`), issued by `AuthController`, validated by `JwtAuthenticationFilter`/`JwtTokenProvider`. Three roles exist: `ROLE_USER`, `ROLE_ADMIN`, `ROLE_SYSTEM` (`RoleName`). Endpoint-level access is enforced either declaratively (`SecurityConfig` public allowlist) or via `@PreAuthorize` on controllers/methods.

---

## Permission legend

| Symbol                  | Meaning                                                                                                                 |
| ----------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| **Public**        | No token required (`SecurityConfig.PUBLIC_ENDPOINTS`)                                                                 |
| **Authenticated** | Any valid JWT (`ROLE_USER`, `ROLE_ADMIN`, or `ROLE_SYSTEM`) — default rule for any request not explicitly public |
| **ROLE_ADMIN**    | Caller's JWT must carry`ROLE_ADMIN` (`AdminController` class-level `@PreAuthorize("hasRole('ADMIN')")`)           |

Ownership scoping: "Authenticated" endpoints under `/api/users/me/**` and `/api/users/me` additionally scope data to the caller — the target user is derived from the JWT subject (email) via `@AuthenticationPrincipal`, never from a path/body parameter, so one user can never read or mutate another user's rows through these routes.

---

## AuthController — `/api/auth`

Tag: Auth. No `@SecurityRequirement` — entirely public per `SecurityConfig.PUBLIC_ENDPOINTS` (`/api/auth/**`).

| Method | Path                   | Permission | Description                                                                                                                                                                                                                                                                                                                                                                                          |
| ------ | ---------------------- | ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| POST   | `/api/auth/register` | Public     | Creates a new user with`ROLE_USER`. If `RegisterRequest` includes a valid admin register code (`app.admin.register-code`), `ROLE_ADMIN` is also granted. Returns `201` + JWT. `409` if email taken.                                                                                                                                                                                      |
| POST   | `/api/auth/login`    | Public     | Two modes in one endpoint: (1) email + password → validates credentials, returns JWT + user profile; (2) email +`systemCode` (matching `app.system.code`), no password → returns a JWT carrying only `ROLE_SYSTEM` (no user profile), meant for service-to-service calls from other components (e.g. provider/workers) that need to call back into manager. `401` on bad credentials/code. |

---

## AccountController — `/api/users`

Tag: Account. `@SecurityRequirement(bearerAuth)`. Self-service only — every method resolves the target user from the authenticated principal's email, not from any client-supplied ID.

| Method | Path                       | Permission    | Description                                                        |
| ------ | -------------------------- | ------------- | ------------------------------------------------------------------ |
| GET    | `/api/users/me`          | Authenticated | Returns the caller's own profile.                                  |
| PUT    | `/api/users/me`          | Authenticated | Updates the caller's own email/name.`409` if new email is taken. |
| PATCH  | `/api/users/me/password` | Authenticated | Changes the caller's own password (stored hashed).                 |

---

## AdminController — `/api/users`

Tag: Admin. Class-level `@PreAuthorize("hasRole('ADMIN')")` — every method requires **ROLE_ADMIN**, in addition to `@SecurityRequirement(bearerAuth)`. Operates on arbitrary users by path `id`, unlike `AccountController`.

| Method | Path                                       | Permission | Description                                                                                                                                                |
| ------ | ------------------------------------------ | ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| GET    | `/api/users/{id}`                        | ROLE_ADMIN | Fetch any user's full profile by ID.`404` if missing.                                                                                                    |
| GET    | `/api/users`                             | ROLE_ADMIN | Paginated list of all users, newest first. Query params:`page` (default 0), `size` (default 20, see `Constants.DEFAULT_PAGE`/`DEFAULT_PAGE_SIZE`). |
| PUT    | `/api/users/{id}`                        | ROLE_ADMIN | Updates a user's email/name/role.`409` on email collision, `404` if missing.                                                                           |
| PATCH  | `/api/users/{id}/password`               | ROLE_ADMIN | Resets a user's password.                                                                                                                                  |
| DELETE | `/api/users/{id}`                        | ROLE_ADMIN | Permanently deletes a user and their data.`204` on success.                                                                                              |
| PUT    | `/api/users/{id}/roles/admin`            | ROLE_ADMIN | Grants`ROLE_ADMIN` to the user (no-op if already held).                                                                                                  |
| DELETE | `/api/users/{id}/roles/admin`            | ROLE_ADMIN | Revokes`ROLE_ADMIN` from the user (no-op if not held).                                                                                                   |
| PATCH  | `/api/users/{id}/enabled?enabled={bool}` | ROLE_ADMIN | Enables/disables an account; disabled users cannot authenticate.                                                                                           |

Note: `AccountController` and `AdminController` share the `/api/users` base path but are disambiguated by route shape (`/me` vs `/{id}`) and are two separate `@RestController` classes with independent permission rules.

---

## NotificationController — `/api/users/me/notifications`

Tag: Notifications. `@SecurityRequirement(bearerAuth)`. Self-service only, scoped to the caller via JWT subject.

| Method | Path                                   | Permission    | Description                                                                                                                                            |
| ------ | -------------------------------------- | ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| GET    | `/api/users/me/notifications`        | Authenticated | Paginated list of all of the caller's notifications (seen + unseen), newest first.`page`/`size` query params, defaults from `Constants`.         |
| GET    | `/api/users/me/notifications/unseen` | Authenticated | Paginated list of only unseen notifications.                                                                                                           |
| PATCH  | `/api/users/me/notifications/seen`   | Authenticated | Marks the given notification IDs (body:`NotificationIdsRequest`) as seen. IDs not owned by the caller are silently ignored — not a `403`/`404`. |
| DELETE | `/api/users/me/notifications`        | Authenticated | Deletes the given notification IDs. Same silent-ignore rule for IDs not owned by the caller.                                                           |

Notifications are never created via HTTP — they are produced exclusively by the RabbitMQ consumer described below (`ArticleNotificationListener` → `NotificationService.createForUsers`).

---

## SubscriptionController — `/api/users/me/subscriptions`

Tag: Subscriptions. `@SecurityRequirement(bearerAuth)`. Self-service only, scoped to the caller via JWT subject.

| Method | Path                                 | Permission    | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| ------ | ------------------------------------ | ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| GET    | `/api/users/me/subscriptions`      | Authenticated | Paginated list of the caller's topic/story subscriptions, newest first.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| POST   | `/api/users/me/subscriptions`      | Authenticated | Subscribes the caller to a topic or story (body:`CreateSubscriptionRequest` — `type` + `targetId`). Before creating the row, `SubscriptionService` synchronously calls the external **news-provider** service (`POST /api/subscriptions/topic/{targetId}` or `/story/{targetId}`, base URL `app.news-provider.base-url`) to verify the target exists. `404` if provider says the target doesn't exist (provider `400` is remapped to `404`), `502`/`ExternalServiceException` if provider is unreachable, `409` if already subscribed. |
| DELETE | `/api/users/me/subscriptions/{id}` | Authenticated | Removes one of the caller's own subscriptions. Also calls the news-provider service (`DELETE .../topic\|story/{targetId}`) to unregister interest. `404` if the subscription doesn't belong to the caller.                                                                                                                                                                                                                                                                                                                                                           |

Note: subscribe/unsubscribe talk to news-provider over plain HTTP (`RestTemplate`), **not** RabbitMQ — the queue traffic only flows the other direction (provider/workers → manager) once an article actually matches, described below.

---

## RabbitMQ integration

Configuration: `RabbitConfig` (`src/main/java/.../config/RabbitConfig.java`), properties in `application.yaml` under `rabbitmq:`.

```yaml
rabbitmq:
  url: amqp://admin:secret@localhost:5672/news_monitor
  scrape-jobs-queue: scrape.jobs
  article-notifications-queue: article.notifications
```

- Connection: single `CachingConnectionFactory` built from `rabbitmq.url`.
- Serialization: `Jackson2JsonMessageConverter` is wired into both the `RabbitTemplate` (send side) and the `SimpleRabbitListenerContainerFactory` (receive side), so message bodies are JSON, mapped straight to/from Java records.
- The manager module declares and binds to exactly one durable queue: `articleNotificationsQueue` (name from `rabbitmq.article-notifications-queue`, default `article.notifications`).
- `scrape-jobs-queue` is present in configuration but **not consumed or published by this module** — it belongs to the provider/worker side of the system (out of scope for this directory).
- A `RabbitTemplate` bean exists but this module currently only **consumes**; it does not publish any messages itself.

### Consumer: `ArticleNotificationListener`

`src/main/java/.../messaging/ArticleNotificationListener.java`

```java
@RabbitListener(queues = "${rabbitmq.article-notifications-queue}")
public void onArticleNotification(ArticleNotificationMessage message)
```

- Listens on `article.notifications`. Upstream producer (outside this directory) publishes one `ArticleNotificationMessage` per new article matched to a topic or story.
- Message payload — `ArticleNotificationMessage` (`dto/messages/ArticleNotificationMessage.java`), a record:

  | Field          | Type                                          | Meaning                                                          |
  | -------------- | --------------------------------------------- | ---------------------------------------------------------------- |
  | `name`       | `String`                                    | The subscribed target's identifier/name (topic name or story id) |
  | `articleUrl` | `String`                                    | URL of the newly matched article                                 |
  | `type`       | `SubscriptionType` (`TOPIC` \| `STORY`) | Whether`name` refers to a topic or a story                     |
- Handling flow:

  1. Looks up all user IDs subscribed to `(name, type)` via `SubscriptionRepository.findUserIdsByTargetIdAndType`.
  2. If no subscribers, logs and returns — no notification rows are created.
  3. Otherwise formats a message (`"New article for %s '%s': %s"` with `type`, `name`, `articleUrl`) and calls `NotificationService.createForUsers(userIds, text)`, which inserts one `Notification` row per subscribed user.
- This is the **only** way `Notification` rows are created — there is no HTTP endpoint to create notifications directly; clients only read/mark-seen/delete via `NotificationController` above.
- No permission model applies here — this is not an HTTP-facing concern. Trust boundary is implicit: any producer with access to the `article.notifications` queue on the shared broker can trigger notifications for any subscriber. There's no message-level authentication/signature check in the listener itself.

---

## Cross-reference: role enforcement summary

| Role            | Granted when                                                                                             | Endpoints it unlocks                                                                                                                                                                                                                |
| --------------- | -------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ROLE_USER`   | Default on`/api/auth/register`                                                                         | All "Authenticated" endpoints above (own account, own notifications, own subscriptions)                                                                                                                                             |
| `ROLE_ADMIN`  | Register with valid`app.admin.register-code`, or granted later via `PUT /api/users/{id}/roles/admin` | Everything`ROLE_USER` unlocks, plus all of `AdminController`                                                                                                                                                                    |
| `ROLE_SYSTEM` | `/api/auth/login` with valid `app.system.code` instead of a password                                 | Same "Authenticated" tier as`ROLE_USER`/`ROLE_ADMIN` from Spring Security's point of view (no user profile attached); intended for other services in this system to call manager's authenticated endpoints without a human user |

---

![1785139534757](image/MANAGER_API/1785139534757.png)
