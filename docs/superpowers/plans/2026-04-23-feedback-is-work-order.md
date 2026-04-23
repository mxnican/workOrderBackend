# Feedback Is Work Order Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `myFeedback` and `workOrder` two role-based views over the same ticket data so user submissions appear in the admin queue, status/replies stay synchronized, and the UI styling stays unchanged.

**Architecture:** Keep `wo_feedback` as the single source of truth for tickets, extend it with the admin-only fields needed for processing, and stop reading/writing the standalone `wo_work_order` seed data. The user-facing routes continue to use `/api/feedback/**`, while admin-facing routes use `/api/work-order/**` but operate on the same underlying row set and reply thread. Frontend components keep their current layout; only the data source and a few fields/actions change.

**Tech Stack:** Spring Boot 3.2.6, JDBC + MySQL 8, Vue 3 + Vite + Element Plus, Maven, Node.js.

---

### Task 1: Prove the current split-data bug with failing tests

**Files:**
- Modify: `D:/wly/workOrderBackend/src/test/java/com/wly/workorder/controller/AuthFlowTest.java`
- Modify: `D:/wly/workOrderBackend/src/test/java/com/wly/workorder/service/MySqlFeedbackIntegrationTest.java`

- [ ] **Step 1: Write the failing test**

Add a test that creates feedback as `user`, then logs in as `admin` and expects the same record code to appear in the admin work-order list. Add a second assertion that an admin status update is visible when the same feedback is fetched again as the user.

```java
@Test
void admin_sees_user_created_feedback_in_work_order_queue() throws Exception {
  String userToken = login("user", "user123");
  String adminToken = login("admin", "admin123");

  MvcResult created = mockMvc.perform(post("/api/feedback")
      .header("Authorization", userToken)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"title\":\"Shared ticket\",\"description\":\"Same row for both roles\",\"accountName\":\"Tester\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(200))
    .andReturn();

  JsonNode createdRoot = objectMapper.readTree(created.getResponse().getContentAsString());
  String feedbackId = createdRoot.path("data").path("id").asText();
  String feedbackCode = createdRoot.path("data").path("code").asText();

  mockMvc.perform(get("/api/work-order/page")
      .header("Authorization", adminToken)
      .param("pageNum", "1")
      .param("pageSize", "20"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(200))
    .andExpect(jsonPath("$.data.records[0].code").value(feedbackCode));

  mockMvc.perform(post("/api/work-order/" + feedbackId + "/status")
      .header("Authorization", adminToken)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"status\":\"PROCESSING\",\"remark\":\"Assigned by admin\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(200));

  mockMvc.perform(get("/api/feedback/" + feedbackId)
      .header("Authorization", userToken))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(200))
    .andExpect(jsonPath("$.data.status").value("PROCESSING"));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
$env:JAVA_HOME='D:\wly\tools\temurin17\jdk-17.0.18+8'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn -q -Dtest=AuthFlowTest test
```

Expected: the new assertion fails because admin work orders still come from the standalone `wo_work_order` seed rows, not from the user-created feedback row.

- [ ] **Step 3: Keep the test as-is and move to implementation**

Do not weaken the assertion. The failure is the proof that the current model is split in the wrong place.

- [ ] **Step 4: Re-run after implementation**

Run the same command and expect PASS.

- [ ] **Step 5: Commit**

```bash
git add D:/wly/workOrderBackend/src/test/java/com/wly/workorder/controller/AuthFlowTest.java D:/wly/workOrderBackend/src/test/java/com/wly/workorder/service/MySqlFeedbackIntegrationTest.java
git commit -m "test: capture shared feedback and work order behavior"
```

### Task 2: Make the backend use one ticket row for both user and admin views

**Files:**
- Modify: `D:/wly/workOrderBackend/src/main/resources/schema.sql`
- Modify: `D:/wly/workOrderBackend/src/main/java/com/wly/workorder/model/TicketModels.java`
- Modify: `D:/wly/workOrderBackend/src/main/java/com/wly/workorder/config/DatabaseSeeder.java`
- Modify: `D:/wly/workOrderBackend/src/main/java/com/wly/workorder/service/impl/JdbcTicketService.java`
- Modify: `D:/wly/workOrderBackend/src/main/java/com/wly/workorder/service/TicketService.java`
- Modify: `D:/wly/workOrderBackend/src/main/java/com/wly/workorder/controller/TicketController.java`
- Modify: `D:/wly/workOrderBackend/src/main/java/com/wly/workorder/auth/AuthInterceptor.java`
- Modify: `D:/wly/workOrderBackend/src/test/java/com/wly/workorder/controller/AuthFlowTest.java`
- Modify: `D:/wly/workOrderBackend/src/test/java/com/wly/workorder/service/MySqlFeedbackIntegrationTest.java`

- [ ] **Step 1: Extend the ticket schema**

Add the admin-processing fields to the same `wo_feedback` row and stop depending on a separate `wo_work_order` seed set.

```sql
alter table wo_feedback
  add column if not exists assignee varchar(128) not null default '';
```

Keep `wo_work_order` in the schema only as a compatibility table for now, but stop reading from it. `wo_feedback` becomes the canonical table for both `/api/feedback/**` and `/api/work-order/**`.

- [ ] **Step 2: Add failing backend behavior tests before changing service code**

Add one test that asserts the admin list is sourced from the user-created feedback row and one test that asserts an admin reply is visible in the user thread.

```java
@Test
void admin_reply_updates_same_ticket_thread() throws Exception {
  String userToken = login("user", "user123");
  String adminToken = login("admin", "admin123");

  MvcResult created = mockMvc.perform(post("/api/feedback")
      .header("Authorization", userToken)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"title\":\"Reply bridge\",\"description\":\"One thread for both roles\",\"accountName\":\"Tester\"}"))
    .andReturn();

  String id = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asText();

  mockMvc.perform(post("/api/work-order/" + id + "/reply")
      .header("Authorization", adminToken)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"content\":\"Admin processing note\",\"author\":\"Support\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(200));

  mockMvc.perform(get("/api/feedback/" + id).header("Authorization", userToken))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.replies[0].content").value("Admin processing note"));
}
```

- [ ] **Step 3: Update the service to read/write one table**

Implement these rules:

* `createFeedback(...)` inserts a row into `wo_feedback` and sets `status = PENDING`, `assignee = ''`.
* `pageFeedback(...)` queries `wo_feedback` for the current user.
* `pageWorkOrders(...)` queries the same `wo_feedback` rows, but the admin projection includes `assignee`.
* `updateWorkOrderStatus(...)` updates the same `wo_feedback` row instead of `wo_work_order`.
* `replyFeedback(...)` and the new admin reply endpoint both append to `wo_feedback_reply`.
* Remove the hard-coded `WO-001` / `WO-002` seed rows from `DatabaseSeeder.java`.

Concrete service mapping:

```java
// work-order list projection comes from wo_feedback
select id, code, title, description, status, assignee, created_at, updated_at
from wo_feedback
```

And the status update uses the same row:

```java
update wo_feedback set status = ?, assignee = ?, updated_at = ? where id = ?
```

- [ ] **Step 4: Add the admin reply route**

Expose `/api/work-order/{id}/reply` for admin processing notes, and keep `/api/feedback/{id}/reply` for the user thread. Both routes call the same service layer so the reply timeline stays in one place.

```java
@PostMapping("/work-order/{id}/reply")
public ApiResponse<Feedback> replyWorkOrder(@PathVariable String id, @RequestBody @Valid ReplyFeedbackRequest request) {
  Feedback feedback = ticketService.replyFeedback(id, request);
  return feedback == null ? ApiResponse.fail("feedback not found") : ApiResponse.success("replied", feedback);
}
```

- [ ] **Step 5: Re-run backend tests**

Run:

```powershell
$env:JAVA_HOME='D:\wly\tools\temurin17\jdk-17.0.18+8'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn -q -Dtest=AuthFlowTest,DatabaseMigrationTest,MySqlFeedbackIntegrationTest test
```

Expected: all tests pass, and the admin/user assertions now point at the same ticket row.

- [ ] **Step 6: Commit**

```bash
git add D:/wly/workOrderBackend/src/main/resources/schema.sql D:/wly/workOrderBackend/src/main/java/com/wly/workorder/model/TicketModels.java D:/wly/workOrderBackend/src/main/java/com/wly/workorder/config/DatabaseSeeder.java D:/wly/workOrderBackend/src/main/java/com/wly/workorder/service/impl/JdbcTicketService.java D:/wly/workOrderBackend/src/main/java/com/wly/workorder/service/TicketService.java D:/wly/workOrderBackend/src/main/java/com/wly/workorder/controller/TicketController.java D:/wly/workOrderBackend/src/main/java/com/wly/workorder/auth/AuthInterceptor.java D:/wly/workOrderBackend/src/test/java/com/wly/workorder/controller/AuthFlowTest.java D:/wly/workOrderBackend/src/test/java/com/wly/workorder/service/MySqlFeedbackIntegrationTest.java
git commit -m "feat: unify feedback and work order data"
```

### Task 3: Update the frontend to read the shared ticket data without changing the styling

**Files:**
- Modify: `D:/wly/workOrderFrontend/src/views/FeedbackView.vue`
- Modify: `D:/wly/workOrderFrontend/src/views/WorkOrderView.vue`
- Modify: `D:/wly/workOrderFrontend/src/api/workOrder.js`
- Modify: `D:/wly/workOrderFrontend/src/api/feedback.js`
- Modify: `D:/wly/workOrderFrontend/src/styles/main.css`
- Modify: `D:/wly/workOrderFrontend/tests/auth.test.js` if any shared helpers change

- [ ] **Step 1: Write the failing frontend behavior check**

Add a tiny pure helper for normalizing a shared ticket row into the admin table shape, then test that it preserves the same `code`, `title`, `status`, and `assignee`.

```js
// src/utils/ticketView.js
export function toWorkOrderRow(ticket) {
  return {
    id: ticket.id,
    code: ticket.code,
    title: ticket.title,
    assignee: ticket.assignee || '',
    status: ticket.status,
    updatedAt: ticket.updatedAt
  }
}
```

Test:

```js
import assert from 'node:assert/strict'
import { toWorkOrderRow } from '../src/utils/ticketView.js'

const row = toWorkOrderRow({
  id: 'fb-1',
  code: 'FB-001',
  title: 'Shared ticket',
  assignee: 'Support A',
  status: 'PROCESSING',
  updatedAt: '2026-04-23 11:00:00'
})

assert.equal(row.code, 'FB-001')
assert.equal(row.assignee, 'Support A')
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
node .\tests\ticketView.test.js
```

Expected: fail because the helper does not exist yet.

- [ ] **Step 3: Implement the helper and wire it into the views**

Use the helper in `WorkOrderView.vue` so the list is just a projection of the shared ticket data. Keep the page layout and Element Plus styling exactly as-is. Add an admin reply action next to status update so admins can leave processing notes in the same thread.

Use the existing `FeedbackView.vue` layout unchanged except for the fact that the replies and status now reflect the same shared ticket row that the admin sees.

- [ ] **Step 4: Re-run the frontend tests and build**

Run:

```powershell
npm test
npm run build
```

Expected: pass with no warnings that change the rendered UI.

- [ ] **Step 5: Commit**

```bash
git add D:/wly/workOrderFrontend/src/views/FeedbackView.vue D:/wly/workOrderFrontend/src/views/WorkOrderView.vue D:/wly/workOrderFrontend/src/api/workOrder.js D:/wly/workOrderFrontend/src/api/feedback.js D:/wly/workOrderFrontend/src/styles/main.css D:/wly/workOrderFrontend/src/utils/ticketView.js D:/wly/workOrderFrontend/tests/ticketView.test.js
git commit -m "feat: render shared ticket data in both frontends"
```

### Task 4: Verify the full cross-role flow end to end

**Files:**
- Test: `D:/wly/workOrderBackend/src/test/java/com/wly/workorder/controller/AuthFlowTest.java`
- Test: `D:/wly/workOrderBackend/src/test/java/com/wly/workorder/service/MySqlFeedbackIntegrationTest.java`
- Test: `D:/wly/workOrderFrontend/tests/auth.test.js`

- [ ] **Step 1: Run backend and frontend verification together**

Run:

```powershell
$env:JAVA_HOME='D:\wly\tools\temurin17\jdk-17.0.18+8'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
cd D:\wly\workOrderBackend
mvn -q test
mvn -q -DskipTests package
cd D:\wly\workOrderFrontend
npm test
npm run build
```

Expected: all commands pass.

- [ ] **Step 2: Manually verify the shared-row behavior**

Use these requests:

```powershell
$login = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/auth/login' -ContentType 'application/json' -Body (@{ username = 'user'; password = 'user123' } | ConvertTo-Json)
$userHeaders = @{ Authorization = "Bearer $($login.data.token)" }

Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/feedback' -Headers $userHeaders -ContentType 'application/json' -Body (@{ title='shared row'; description='same record'; accountName='Tester' } | ConvertTo-Json)

$admin = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/auth/login' -ContentType 'application/json' -Body (@{ username = 'admin'; password = 'admin123' } | ConvertTo-Json)
$adminHeaders = @{ Authorization = "Bearer $($admin.data.token)" }

Invoke-RestMethod -Method Get -Uri 'http://127.0.0.1:8080/api/work-order/page?pageNum=1&pageSize=20' -Headers $adminHeaders
```

Expected: the admin list contains the same `FB-...` row the user created.

- [ ] **Step 3: Commit the finished feature**

```bash
git add D:/wly/workOrderBackend D:/wly/workOrderFrontend
git commit -m "feat: unify feedback and work order into one shared ticket flow"
```

## Self-Review

Coverage check:
- The split-data bug is captured in Task 1.
- The backend single-source-of-truth change is fully covered in Task 2.
- The frontend projection and UI continuity are covered in Task 3.
- End-to-end verification is covered in Task 4.

Placeholder scan:
- No TBD/TODO placeholders remain.
- Every implementation task names exact files and concrete API/data changes.

Consistency check:
- `wo_feedback` is the canonical table in every backend task.
- Both `/api/feedback/**` and `/api/work-order/**` operate on the same row set.
- The frontend keeps its existing styling and route split.
