package com.bank.workflow.api;

import com.bank.common.dto.ApiResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thin REST facade over Camunda. The Cockpit/Tasklist webapps are also enabled at
 * /camunda/ for admin use, but the UI calls this controller for typed JSON.
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final IdentityService identityService;

    public record LoanRequest(@NotNull UUID customerId, @NotNull BigDecimal amount, String currency, Integer termMonths) {}

    @PostMapping("/loans")
    public ApiResponse<Map<String, Object>> startLoan(@RequestBody LoanRequest body) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("customerId", body.customerId().toString());
        vars.put("amount", body.amount().longValueExact());
        vars.put("currency", body.currency() == null ? "USD" : body.currency());
        vars.put("termMonths", body.termMonths() == null ? 36 : body.termMonths());
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("loan-application",
                "loan-" + UUID.randomUUID(), vars);
        return ApiResponse.ok(Map.of(
                "processInstanceId", pi.getId(),
                "businessKey", pi.getBusinessKey()
        ), "Loan application started");
    }

    public record HighValueRequest(@NotNull UUID transferId, @NotNull UUID customerId,
                                   @NotNull BigDecimal amount, String currency) {}

    public record ClosureRequest(@NotNull UUID accountId, @NotNull UUID customerId, String coolingOffDuration) {}

    @PostMapping("/account-closures")
    public ApiResponse<Map<String, Object>> startClosure(@RequestBody ClosureRequest body) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("accountId", body.accountId().toString());
        vars.put("customerId", body.customerId().toString());
        vars.put("coolingOffDuration", body.coolingOffDuration() == null ? "P7D" : body.coolingOffDuration());
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("account-closure",
                "close-" + body.accountId(), vars);
        return ApiResponse.ok(Map.of("processInstanceId", pi.getId()), "Account closure started");
    }

    @PostMapping("/account-closures/{accountId}/cancel")
    public ApiResponse<Void> cancelClosure(@PathVariable UUID accountId) {
        runtimeService.createMessageCorrelation("CancelClosure")
                .processInstanceBusinessKey("close-" + accountId)
                .correlate();
        return ApiResponse.ok(null, "Cancellation message sent");
    }

    public record DisputeRequest(@NotNull UUID transferId, @NotNull UUID customerId,
                                 @NotNull BigDecimal amount, String currency, String reason) {}

    @PostMapping("/disputes")
    public ApiResponse<Map<String, Object>> startDispute(@RequestBody DisputeRequest body) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("transferId", body.transferId().toString());
        vars.put("customerId", body.customerId().toString());
        vars.put("amount", body.amount().longValueExact());
        vars.put("currency", body.currency() == null ? "USD" : body.currency());
        vars.put("reason", body.reason());
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("dispute-resolution",
                "dispute-" + UUID.randomUUID(), vars);
        return ApiResponse.ok(Map.of("processInstanceId", pi.getId()), "Dispute case opened");
    }

    @PostMapping("/high-value-approval")
    public ApiResponse<Map<String, Object>> startHighValue(@RequestBody HighValueRequest body) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("transferId", body.transferId().toString());
        vars.put("customerId", body.customerId().toString());
        vars.put("amount", body.amount().longValueExact());
        vars.put("currency", body.currency() == null ? "USD" : body.currency());
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("high-value-approval",
                "hv-" + body.transferId(), vars);
        return ApiResponse.ok(Map.of("processInstanceId", pi.getId()),
                "High-value approval started");
    }

    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> listMyTasks(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        // Bound by JwtRoleGroupFilter: returns only tasks visible to the caller's
        // groups (mapped from JWT roles). Anonymous calls see nothing.
        var auth = identityService.getCurrentAuthentication();
        var query = taskService.createTaskQuery().active().orderByTaskCreateTime().desc();
        if (auth != null && auth.getGroupIds() != null && !auth.getGroupIds().isEmpty()) {
            query = query.taskCandidateGroupIn(auth.getGroupIds()).includeAssignedTasks();
        } else if (userId != null) {
            query = query.taskAssignee(userId);
        } else {
            return ApiResponse.ok(List.of());
        }
        List<Task> tasks = query.list();
        var out = tasks.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("processInstanceId", t.getProcessInstanceId());
            m.put("createTime", t.getCreateTime());
            m.put("dueDate", t.getDueDate());
            m.put("variables", taskService.getVariables(t.getId()));
            return m;
        }).toList();
        return ApiResponse.ok(out);
    }

    public record CompleteTaskRequest(Map<String, Object> variables) {}

    @PostMapping("/tasks/{taskId}/complete")
    public ApiResponse<Void> complete(@PathVariable String taskId, @RequestBody CompleteTaskRequest body) {
        taskService.complete(taskId, body.variables() == null ? Map.of() : body.variables());
        return ApiResponse.ok(null, "Task completed");
    }

    @PostMapping("/tasks/{taskId}/claim")
    public ApiResponse<Void> claim(@PathVariable String taskId, @RequestHeader("X-User-Id") String userId) {
        taskService.claim(taskId, userId);
        return ApiResponse.ok(null, "Task claimed");
    }
}
