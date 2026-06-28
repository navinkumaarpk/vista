# VISTA Agent Roles

- **Monitor** — polls telemetry on a tunable schedule, cheaply screens for
  degradation, and triggers proactive triage on suspect service groups.
- **Observe** — deterministically gathers stats and tickets for a service group.
- **Retrieval** — fetches semantically similar historical incidents from pgvector.
- **Reasoning (ToT)** — generates PLANT/CPE/PROVISIONING hypotheses as parallel branches.
- **Critic** — scores each branch (signal, retrieval, scope, resolution); beam search selects.
- **Decide** — maps the selected classification to an action and escalation flag.
- **Guardrails** — enforce latency cap, human-review gates, and metrics/calibration.