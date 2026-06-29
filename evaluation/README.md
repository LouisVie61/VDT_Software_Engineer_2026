$env:APP_LLM_PROVIDER_ORDER = "GEMINI"
$env:GEMINI_MODEL = "gemini-2.5-flash"
if ([string]::IsNullOrWhiteSpace($env:GEMINI_API_KEY)) { throw "Set GEMINI_API_KEY in this shell first." }
mvn spring-boot:run

# Run this command in a second PowerShell window after port 8080 is ready.
python evaluation\runner.py `
  --variant llm-baseline `
  --base-url http://localhost:8080 `
  --cases evaluation\cases\ablation\ablation_cases.jsonl `
  --require-executions 28 `
  --repeat 1 `
  --warmup 0 `
  --timeout 300 `
  --data-snapshot soc-events-gP5CmfSDQcmg0NgqorwMqA-docs100003 `
  --provider-config gemini-2.5-flash `
  --cache-regime cold `
  --output evaluation\reports\ab_baseline.md `
  --json-output evaluation\reports\ab_baseline.json
