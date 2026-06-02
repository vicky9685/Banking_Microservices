package com.bank.workflow.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Fires when the SLA timer trips on the approver user task.
 * Reassigns the task to the senior risk officer group instead of cancelling.
 */
@Slf4j
@Component("escalationDelegate")
public class EscalationDelegate implements JavaDelegate {

    private final TaskService taskService;

    public EscalationDelegate(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void execute(DelegateExecution exec) {
        String pi = exec.getProcessInstanceId();
        taskService.createTaskQuery()
                .processInstanceId(pi)
                .active()
                .list()
                .forEach(t -> {
                    taskService.addCandidateGroup(t.getId(), "senior-risk-officers");
                    taskService.setPriority(t.getId(), 100);
                });
        log.warn("Escalated approval pi={} to senior-risk-officers", pi);
    }
}
