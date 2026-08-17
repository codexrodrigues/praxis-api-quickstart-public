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
from datetime import datetime, timezone

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
GOVERNANCE_LAB_BOOTSTRAP = os.environ.get(
    "HOSTED_FIXTURE_GOVERNANCE_LAB_BOOTSTRAP", "false"
).lower() == "true"
TENANT = os.environ.get("HOSTED_FIXTURE_TENANT", "").strip()
ENVIRONMENT = os.environ.get("HOSTED_FIXTURE_ENVIRONMENT", "").strip()
APP_JAR = pathlib.Path(required("HOSTED_FIXTURE_APP_JAR"))
OUTPUT = pathlib.Path(required("HOSTED_FIXTURE_OUTPUT"))
if not APP_JAR.is_file():
    raise SystemExit("HOSTED_FIXTURE_APP_JAR must reference the packaged Quickstart jar")

if GOVERNANCE_LAB_BOOTSTRAP:
    publisher_credentials = (
        required("HOSTED_FIXTURE_PUBLISHER_USERNAME"),
        required("HOSTED_FIXTURE_PUBLISHER_PASSWORD"),
    )
    IDENTITIES = {key: publisher_credentials for key in ("author", "approverA", "approverB", "publisher")}
    APPROVER_USERNAMES = {
        "approverA": os.environ.get(
            "HOSTED_FIXTURE_APPROVER_A_USERNAME", "praxis-governance-approver-a"
        ).strip(),
        "approverB": os.environ.get(
            "HOSTED_FIXTURE_APPROVER_B_USERNAME", "praxis-governance-approver-b"
        ).strip(),
    }
else:
    if not TENANT or not ENVIRONMENT:
        raise SystemExit("HOSTED_FIXTURE_TENANT and HOSTED_FIXTURE_ENVIRONMENT are required")
    IDENTITIES = {
        "author": (required("HOSTED_FIXTURE_AUTHOR_USERNAME"), required("HOSTED_FIXTURE_AUTHOR_PASSWORD")),
        "approverA": (required("HOSTED_FIXTURE_APPROVER_A_USERNAME"), required("HOSTED_FIXTURE_APPROVER_A_PASSWORD")),
        "approverB": (required("HOSTED_FIXTURE_APPROVER_B_USERNAME"), required("HOSTED_FIXTURE_APPROVER_B_PASSWORD")),
        "publisher": (required("HOSTED_FIXTURE_PUBLISHER_USERNAME"), required("HOSTED_FIXTURE_PUBLISHER_PASSWORD")),
    }
    if len({username for username, _ in IDENTITIES.values()}) != 4:
        raise SystemExit("Author, publisher and both composition approvers must be distinct")
    APPROVER_USERNAMES = {
        "approverA": IDENTITIES["approverA"][0],
        "approverB": IDENTITIES["approverB"][0],
    }

COMMON_HEADERS = {
    "Accept": "application/json",
    "Origin": ORIGIN,
}
if TENANT:
    COMMON_HEADERS["X-Tenant-ID"] = TENANT
if ENVIRONMENT:
    COMMON_HEADERS["X-Env"] = ENVIRONMENT


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
            safe_message = result.body.get("message") if isinstance(result.body, dict) else None
            raise RuntimeError(
                f"HTTP {result.status} for {method} {path}; code={safe_code}; message={safe_message}"
            )
        return result

    def login(self, username: str, password: str):
        self.request("POST", "/auth/login", {"username": username, "password": password}, expected=(200, 204))

    def switch_governance_identity(self, identity_key: str):
        self.request(
            "POST", f"/auth/governance-lab/session/{identity_key}", expected=(200, 204)
        )


clients = {key: Client() for key in IDENTITIES}
for key, client in clients.items():
    client.login(*IDENTITIES[key])
if GOVERNANCE_LAB_BOOTSTRAP:
    for client_key, identity_key in (
        ("author", "author"),
        ("approverA", "approver-a"),
        ("approverB", "approver-b"),
    ):
        clients[client_key].switch_governance_identity(identity_key)


def read_head():
    query = urllib.parse.quote(RULE_SET_KEY, safe="")
    return clients["publisher"].request(
        "GET",
        f"/api/praxis/config/domain-rules/snapshots/head?ruleSetKey={query}",
        expected=(200, 404),
    )


INITIAL_HEAD = read_head()


def approved_definition(rule_key: str, reviewer_key: str, operation: str, condition) -> str:
    query = urllib.parse.quote(rule_key, safe="")
    definitions = clients["publisher"].request(
        "GET", f"/api/praxis/config/domain-rules/definitions?ruleKey={query}", expected=(200,)
    ).body
    if INITIAL_HEAD.status == 200:
        bound_sources = [
            source
            for source in INITIAL_HEAD.body.get("snapshot", {}).get("sources", [])
            if source.get("definitionKey") == rule_key
        ]
        if len(bound_sources) != 1 or not bound_sources[0].get("definitionId"):
            raise RuntimeError(f"Published payroll head does not bind exactly one source for {rule_key}")
        return bound_sources[0]["definitionId"]

    known_versions = [item.get("version") for item in definitions if isinstance(item.get("version"), int)]
    next_version = max(known_versions, default=0) + 1

    reviewer = APPROVER_USERNAMES[reviewer_key]
    request = {
        "ruleKey": rule_key,
        "version": next_version,
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
    created = clients["author"].request(
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


def validate_head(response: Response, expected_version: int):
    if response.status != 200:
        raise RuntimeError("Expected a published payroll snapshot head")
    snapshot = response.body.get("snapshot", {})
    expected = payload(expected_version)
    if TENANT and snapshot.get("tenantId") != TENANT:
        raise RuntimeError("Published payroll head escaped the requested tenant/environment scope")
    if ENVIRONMENT and snapshot.get("environment") != ENVIRONMENT:
        raise RuntimeError("Published payroll head escaped the requested tenant/environment scope")
    if not snapshot.get("tenantId") or not snapshot.get("environment"):
        raise RuntimeError("Published payroll head did not resolve a server-owned scope")
    if snapshot.get("ownerServiceKey") != OWNER_SERVICE_KEY:
        raise RuntimeError("Published payroll head has an unexpected owner")
    if snapshot.get("requiredHostContractVersion") != HOST_CONTRACT_VERSION:
        raise RuntimeError("Published payroll head has an unexpected host contract")
    if snapshot.get("ruleSet") != expected.get("ruleSet"):
        raise RuntimeError("Published payroll head differs from the canonical host RuleSet")
    if sorted(item.get("definitionId") for item in snapshot.get("sources", [])) != sorted((net_id, date_id)):
        raise RuntimeError("Published payroll head is not bound to the approved hosted definitions")


def head_is_effective(response: Response) -> bool:
    snapshot = response.body.get("snapshot", {})
    valid_until = snapshot.get("validUntilUtc")
    if not valid_until:
        return True
    return datetime.fromisoformat(valid_until.replace("Z", "+00:00")) > datetime.now(timezone.utc)


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
    clients["publisher"].request(
        "POST", "/api/praxis/config/domain-rules/snapshots", candidate,
        headers=precondition, expected=(201,)
    )
    return read_head()


current = INITIAL_HEAD
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
    elif GOVERNANCE_LAB_BOOTSTRAP and isinstance(current_version, int) and current_version >= 2:
        validate_head(current, current_version)
        if not head_is_effective(current):
            current = publish(current_version + 1, current)
            status = "RENEWED"
    elif current_version != 2:
        raise RuntimeError("Hosted payroll fixture found an incompatible active RuleSet version")

resolved_version = current.body["snapshot"]["ruleSet"]["ref"]["version"]
validate_head(current, resolved_version)
resolved_tenant = current.body["snapshot"]["tenantId"]
resolved_environment = current.body["snapshot"]["environment"]
catalog = clients["publisher"].request(
    "GET",
    "/api/praxis/config/domain-rules/snapshots?ruleSetKey=" + urllib.parse.quote(RULE_SET_KEY, safe="") + "&limit=10",
    expected=(200,),
).body
catalog_versions = sorted({
    item.get("ruleSetVersion")
    for item in catalog
    if isinstance(item.get("ruleSetVersion"), int)
})
if not GOVERNANCE_LAB_BOOTSTRAP and catalog_versions != [1, 2]:
    raise RuntimeError("Hosted payroll fixture requires exactly one immutable v1 and one immutable v2")
if GOVERNANCE_LAB_BOOTSTRAP and resolved_version not in catalog_versions:
    raise RuntimeError("Persistent payroll fixture catalog does not contain its active immutable version")

safe = {
    "schemaVersion": "praxis-payroll-snapshot-provision/v2",
    "status": status,
    "serviceBaseUrlHash": hashlib.sha256(BASE_URL.encode()).hexdigest(),
    "tenantHash": hashlib.sha256(resolved_tenant.encode()).hexdigest(),
    "environment": resolved_environment,
    "ruleSetKey": RULE_SET_KEY,
    "ruleSetVersion": resolved_version,
    "snapshotKey": current.body["snapshot"]["snapshotKey"],
    "snapshotContentHash": current.body["snapshotContentHash"],
    "activationRevision": current.body["activationRevision"],
    "headEtagHash": hashlib.sha256(current.body["headEtag"].encode()).hexdigest(),
    "distinctDefinitionApprovers": 2,
    "verifiedImmutableVersions": catalog_versions,
}
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
OUTPUT.write_text(json.dumps(safe, indent=2) + "\n")
OUTPUT.chmod(0o600)
print(json.dumps({"status": status, "ruleSetVersion": resolved_version, "versions": catalog_versions}))
