package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;

/** Generates the canonical QL-07 publication request from the host-owned RuleSet factory. */
public final class RuleLabQl07SnapshotPayload {
    private RuleLabQl07SnapshotPayload() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected two approved source definition UUIDs.");
        }
        UUID.fromString(args[0]);
        UUID.fromString(args[1]);

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ObjectNode request = mapper.createObjectNode();
        request.set("ruleSet", mapper.valueToTree(ExtraordinaryGrantRuleSetFactory.definition()));
        ArrayNode sources = request.putArray("sourceDefinitionIds");
        sources.add(args[0]);
        sources.add(args[1]);
        request.put("ownerServiceKey", ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY);
        request.put("requiredHostContractVersion", ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION);
        request.put("validFromUtc", "2026-01-01T00:00:00Z");
        request.putNull("validUntilUtc");
        request.put("publishedBy", "ql07-release-manager");
        System.out.println(mapper.writeValueAsString(request));
    }
}
