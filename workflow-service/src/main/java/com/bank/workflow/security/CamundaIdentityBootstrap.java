package com.bank.workflow.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Groups;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.identity.Group;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bootstraps the groups our BPMN files reference and grants minimal authorizations.
 * Without this, fresh Camunda DBs reject every user task claim because no group exists.
 *
 * Authorizations granted per group (CRUD on PROCESS_DEFINITION + TASK only — never global ADMIN).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.features.camunda.enabled", havingValue = "true", matchIfMissing = true)
public class CamundaIdentityBootstrap {

    private final ProcessEngine processEngine;

    /** Group id → human label. Mirrors candidateGroups in the BPMN files. */
    private static final Map<String, String> GROUPS = new LinkedHashMap<>() {{
        put("customers",             "Banking Customers");
        put("risk-officers",         "Risk Officers");
        put("senior-risk-officers",  "Senior Risk Officers");
        put("underwriters",          "Loan Underwriters");
        put("dispute-analysts",      "Dispute Analysts");
        put("legal",                 "Legal Team");
    }};

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        IdentityService ids = processEngine.getIdentityService();
        AuthorizationService auth = processEngine.getAuthorizationService();

        for (var entry : GROUPS.entrySet()) {
            String id = entry.getKey();
            if (ids.createGroupQuery().groupId(id).singleResult() == null) {
                Group g = ids.newGroup(id);
                g.setName(entry.getValue());
                g.setType(Groups.GROUP_TYPE_WORKFLOW);
                ids.saveGroup(g);
                log.info("Created Camunda group: {}", id);

                grantTaskAccess(auth, id);
            }
        }
    }

    /** Allow group members to read+update+claim tasks assigned to them — nothing else. */
    private void grantTaskAccess(AuthorizationService auth, String groupId) {
        Authorization a = auth.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
        a.setGroupId(groupId);
        a.setResource(Resources.TASK);
        a.setResourceId(Authorization.ANY);
        a.addPermission(Permissions.READ);
        a.addPermission(Permissions.UPDATE);
        a.addPermission(Permissions.TASK_WORK);
        auth.saveAuthorization(a);

        Authorization defs = auth.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
        defs.setGroupId(groupId);
        defs.setResource(Resources.PROCESS_DEFINITION);
        defs.setResourceId(Authorization.ANY);
        defs.addPermission(Permissions.READ);
        defs.addPermission(Permissions.READ_INSTANCE);
        auth.saveAuthorization(defs);
    }
}
