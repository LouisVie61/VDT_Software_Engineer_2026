$ErrorActionPreference = "Stop"

python evaluation\generate_v3_cases.py
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

python evaluation\generate_v3_cases.py --check
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

python -m unittest discover -s evaluation -p "test_*.py"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

python evaluation\runner.py @args
exit $LASTEXITCODE
