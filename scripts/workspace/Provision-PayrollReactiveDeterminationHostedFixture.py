#!/usr/bin/env python3
"""Idempotently provision and verify the Quickstart-owned payroll snapshot aggregate."""

from __future__ import annotations

import hashlib
import http.cookiejar
import json
import os
import pathlib
import ssl
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request

RULE_SET_KEY = "human-resources.payroll.reactive-determinations"
OWNER_SERVICE_KEY = "praxis-api-quickstart"
HOST_CONTRACT_VERSION = "quickstart/1.0"
NET_KEY = "human-resources.payroll.net-salary"
DATE_KEY = "human-resources.payroll.payment-date"


def required(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise SystemExit(f"Missing required environment variable: {name}")
    return value


BASE_URL = required("HOSTED_FIXTURE_BASE_URL").rstrip("/")
ORIGIN = required("HOSTED_FIXTURE_ORIGIN")
TENANT = required("HOSTED_FIXTURE_TENANT")
ENVIRONMENT = required("HOSTED_FIXTURE_ENVIRONMENT")
APP_JAR = pathlib.Path(required("HOSTED_FIXTURE_APP_JAR"))
OUTPUT = pathlib.Path(required("HOSTED_FIXTURE_OUTPUT"))
if not APP_JAR.is_file():
    raise SystemExit("HOSTED_FIXTURE_APP_JAR must reference the packaged Quickstart jar")

IDENTITIES = {
    "approverA": (required("HOSTED_FIXTURE_APPROVER_A_USERNAME"), required("HOSTED_FIXTURE_APPROVER_A_PASSWORD")),
    "approverB": (required("HOSTED_FIXTURE_APPROVER_B_USERNAME"), required("HOSTED_FIXTURE_APPROVER_B_PASSWORD")),
    "publisher": (required("HOSTED_FIXTURE_PUBLISHER_USERNAME"), required("HOSTED_FIXTURE_PUBLISHER_PASSWORD")),
}
if len({username for username, _ in IDENTITIES.values()}) != 3:
    raise SystemExit("Publisher and both composition approvers must be distinct")

COMMON_HEADERS = {
    "Accept": "application/json",
    "Origin": ORIGIN,
    "X-Tenant-ID": TENANT,
    "X-Env": ENVIRONMENT,
}


class Response:
    def __init__(self, status: int, body, headers):
        self.status = status
        self.body = body
        self.headers = headers


class Client:
    def __init__(self):
        jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(jar),
            urllib.request.HTTPSHandler(context=ssl.create_default_context()),
        )

    def request(self, method: str, path: str, body=None, headers=None, expected=(200,)) -> Response:
        merged = dict(COMMON_HEADERS)
        if headers:
            merged.update(headers)
        data = None
        if body is not None:
            data = json.dumps(body, separators=(",", ":")).encode()
            merged["Content-Type"] = "application/json"
        request = urllib.request.Request(BASE_URL + path, data=data, headers=merged, method=method)
        try:
            with self.opener.open(request, timeout=60) as response:
                raw = response.read()
                parsed = json.loads(raw) if raw else None
                result = Response(response.status, parsed, dict(response.headers))
        except urllib.error.HTTPError as error:
            raw = error.read()
            try:
                parsed = json.loads(raw) if raw else None
            except json.JSONDecodeError:
                parsed = None
            result = Response(error.code, parsed, dict(error.headers))
        if result.status not in expected:
            safe_code = result.body.get("code") if isinstance(result.body, dict) else None
            raise RuntimeError(f"HTTP {result.status} for {method} {path}; code={safe_code}")
        return result

    def login(self, username: str, password: str):
        self.request("POST", "/auth/login", {"username": username, "password": password}, expected=(204,))


clients = {key: Client() for key in IDENTITIES}
for key, client in clients.items():
    client.login(*IDENTITIES[key])


def approved_definition(rule_key: str, reviewer_key: str, operation: str, condition) -> str:
    query = urllib.parse.quote(rule_key, safe="")
    definitions = clients["publisher"].request(
        "GET", f"/api/praxis/config/domain-rules/definitions?ruleKey={query}", expected=(200,)
    ).body
    approved = [item for item in definitions if item.get("status") in ("approved", "active")]
    if len(approved) > 1:
        versions = {item.get("version") for item in approved}
        if len(versions) != len(approved):
            raise RuntimeError(f"Ambiguous approved definition history for {rule_key}")
    if approved:
        return max(approved, key=lambda item: item["version"])["id"]

    reviewer = IDENTITIES[reviewer_key][0]
    request = {
        "ruleKey": rule_key,
        "version": 1,
        "ruleType": "selection_eligibility",
        "status": "draft",
        "contextKey": "human-resources",
        "resourceKey": RULE_SET_KEY,
        "serviceKey": OWNER_SERVICE_KEY,
        "semanticOwner": "praxis-rules-engine",
        "steward": "hosted-proof",
        "definition": {"kind": "reactive_determination", "operationId": operation},
        "parameters": {
            "hostContractVersion": HOST_CONTRACT_VERSION,
            "bindingOrder": 10 if reviewer_key == "approverA" else 20,
        },
        "condition": condition,
        "governance": {
            "lifecycleBoundary": "HOSTED_PROOF_ONLY",
            "sourceKind": "DISPOSABLE_CORPORATE_FIXTURE",
            "sourceRuleSetVersion": 1,
            "authorityChangeAllowed": False,
            "requiredApprovals": [reviewer],
            "authorizedApprovers": [reviewer],
            "auditReason": "Disposable payroll aggregate LKG proof",
        },
    }
    created = clients["publisher"].request(
        "POST", "/api/praxis/config/domain-rules/definitions", request, expected=(202,)
    ).body
    transition = {
        "status": "approved",
        "validationResult": {"valid": True, "approvalReason": "Reviewed hosted payroll fixture"},
    }
    approved = clients[reviewer_key].request(
        "PATCH",
        f"/api/praxis/config/domain-rules/definitions/{created['id']}/status",
        transition,
        expected=(200,),
    ).body
    if approved.get("status") not in ("approved", "active"):
        raise RuntimeError("Definition approval did not reach an approved state")
    return approved["id"]


net_id = approved_definition(
    NET_KEY,
    "approverA",
    "determinePayrollNetSalary",
    {">=": [{"var": "payroll.salarioBruto"}, {"var": "payroll.totalDescontos"}]},
)
date_id = approved_definition(
    DATE_KEY,
    "approverB",
    "determinePayrollPaymentDate",
    {"and": [{">=": [{"var": "payroll.ano"}, 1900]}, {">=": [{"var": "payroll.mes"}, 1]}]},
)


def payload(version: int):
    raw = subprocess.check_output(
        [
            "java",
            "-Dloader.main=com.example.praxis.apiquickstart.config.PayrollReactiveDeterminationFixturePayload",
            "-cp",
            str(APP_JAR),
            "org.springframework.boot.loader.launch.PropertiesLauncher",
            net_id,
            date_id,
            str(version),
        ],
        text=True,
        stderr=subprocess.DEVNULL,
        timeout=30,
    )
    return json.loads(raw.strip().splitlines()[-1])


def head():
    query = urllib.parse.quote(RULE_SET_KEY, safe="")
    return clients["publisher"].request(
        "GET",
        f"/api/praxis/config/domain-rules/snapshots/head?ruleSetKey={query}",
        expected=(200, 404),
    )


def validate_head(response: Response, expected_version: int):
    if response.status != 200:
        raise RuntimeError("Expected a published payroll snapshot head")
    snapshot = response.body.get("snapshot", {})
    expected = payload(expected_version)
    if snapshot.get("tenantId") != TENANT or snapshot.get("environment") != ENVIRONMENT:
        raise RuntimeError("Published payroll head escaped the requested tenant/environment scope")
    if snapshot.get("ownerServiceKey") != OWNER_SERVICE_KEY:
        raise RuntimeError("Published payroll head has an unexpected owner")
    if snapshot.get("requiredHostContractVersion") != HOST_CONTRACT_VERSION:
        raise RuntimeError("Published payroll head has an unexpected host contract")
    if snapshot.get("ruleSet") != expected.get("ruleSet"):
        raise RuntimeError("Published payroll head differs from the canonical host RuleSet")
    if sorted(item.get("definitionId") for item in snapshot.get("sources", [])) != sorted((net_id, date_id)):
        raise RuntimeError("Published payroll head is not bound to the approved hosted definitions")


def publish(version: int, current: Response | None):
    candidate = payload(version)
    manifest = clients["publisher"].request(
        "POST", "/api/praxis/config/domain-rules/snapshots/composition-manifest", candidate, expected=(200,)
    ).body
    for reviewer_key in ("approverA", "approverB"):
        approval = clients[reviewer_key].request(
            "POST", "/api/praxis/config/domain-rules/snapshots/composition-approvals", candidate, expected=(200,)
        ).body
        if approval.get("evidenceHash") != manifest.get("compositionDigest"):
            raise RuntimeError("Composition approval is not bound to the canonical manifest")
    candidate["compositionDigest"] = manifest["compositionDigest"]
    precondition = {"If-None-Match": "*"} if current is None else {
        "If-Match": current.headers.get("ETag", f'"{current.body["headEtag"]}"')
    }
    return clients["publisher"].request(
        "POST", "/api/praxis/config/domain-rules/snapshots", candidate,
        headers=precondition, expected=(201,)
    )


current = head()
status = "VERIFIED_EXISTING"
if current.status == 404:
    v1 = publish(1, None)
    current = publish(2, v1)
    status = "PROVISIONED"
else:
    current_version = current.body.get("snapshot", {}).get("ruleSet", {}).get("ref", {}).get("version")
    if current_version == 1:
        validate_head(current, 1)
        current = publish(2, current)
        status = "RESUMED"
    elif current_version != 2:
        raise RuntimeError("Hosted payroll fixture found an incompatible active RuleSet version")

validate_head(current, 2)
catalog = clients["publisher"].request(
    "GET",
    "/api/praxis/config/domain-rules/snapshots?ruleSetKey=" + urllib.parse.quote(RULE_SET_KEY, safe="") + "&limit=10",
    expected=(200,),
).body
versions = sorted(item.get("ruleSetVersion") for item in catalog if item.get("ruleSetVersion") in (1, 2))
if versions != [1, 2]:
    raise RuntimeError("Hosted payroll fixture requires exactly one immutable v1 and one immutable v2")

safe = {
    "schemaVersion": "praxis-payroll-snapshot-provision/v2",
    "status": status,
    "serviceBaseUrlHash": hashlib.sha256(BASE_URL.encode()).hexdigest(),
    "tenantHash": hashlib.sha256(TENANT.encode()).hexdigest(),
    "environment": ENVIRONMENT,
    "ruleSetKey": RULE_SET_KEY,
    "ruleSetVersion": 2,
    "snapshotKey": current.body["snapshot"]["snapshotKey"],
    "snapshotContentHash": current.body["snapshotContentHash"],
    "activationRevision": current.body["activationRevision"],
    "headEtagHash": hashlib.sha256(current.body["headEtag"].encode()).hexdigest(),
    "distinctDefinitionApprovers": 2,
    "verifiedImmutableVersions": versions,
}
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
OUTPUT.write_text(json.dumps(safe, indent=2) + "\n")
OUTPUT.chmod(0o600)
print(json.dumps({"status": status, "ruleSetVersion": 2, "versions": versions}))
