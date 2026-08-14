package com.example.praxis.apiquickstart.rulelab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.Arrays;
import org.praxisplatform.config.dto.DomainRuleDefinitionResponse;

/** Generates the canonical QL-07 publication request from the host-owned RuleSet factory. */
public final class RuleLabQl07SnapshotPayload {
    private RuleLabQl07SnapshotPayload() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Expected the approved source-definition JSON file and the next RuleSet version.");
        }
        int ruleSetVersion = Integer.parseInt(args[1]);

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        var definitions = Arrays.asList(mapper.readValue(
                Path.of(args[0]).toFile(), DomainRuleDefinitionResponse[].class));
        var candidate = ExtraordinaryGrantRuleSetComposer.compose(ruleSetVersion, definitions);
        ObjectNode request = mapper.createObjectNode();
        request.set("ruleSet", mapper.valueToTree(candidate.ruleSet()));
        request.set("sourceDefinitionIds", mapper.valueToTree(candidate.sourceDefinitionIds()));
        request.put("ownerServiceKey", ExtraordinaryGrantRuleSnapshotRuntime.OWNER_SERVICE_KEY);
        request.put("requiredHostContractVersion", ExtraordinaryGrantRuleSnapshotRuntime.HOST_CONTRACT_VERSION);
        request.put("validFromUtc", "2026-01-01T00:00:00Z");
        request.putNull("validUntilUtc");
        System.out.println(mapper.writeValueAsString(request));
    }
}
