"""Fixed invented financial contracts, compared with actual Kotlin policy outputs.

Development only. No SMS/database/network/model access or arbitrary input paths.
"""
import copy
from decimal import Decimal
import hashlib
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
FIXTURE = ROOT / 'tools/python_lab/fixtures/financial_contracts.json'
BUILD = ROOT / 'app/build/python-lab'
MONEY = re.compile(r'(\b(?:Ksh|KES)\s+)([0-9][0-9,]*(?:\.[0-9]{1,2})?)')
GROUPS = {'ownership', 'transfer', 'debt', 'merchant'}


def generate():
    raw = FIXTURE.read_bytes()
    fixtures = json.loads(raw)
    if fixtures['schema_version'] != 1 or fixtures['synthetic_only'] is not True:
        raise ValueError('Reviewed invented fixtures required')
    cases = []
    for fixture in fixtures['cases']:
        if fixture['group'] not in GROUPS:
            raise ValueError('Unknown financial contract group')
        for scale in range(1, 9):
            case = copy.deepcopy(fixture)
            case['id'] = f"{fixture['id']}-{scale:02}"
            case['fixture_id'] = fixture['id']
            case['scale'] = scale
            for step in case['steps']:
                if 'text' in step:
                    step['text'] = MONEY.sub(lambda m: m[1] + f"{Decimal(m[2].replace(',', '')) * scale:,.2f}", step['text'])
                for field in ('amount', 'outstanding', 'credit'):
                    value = step['expected'].get(field)
                    if value is not None and value >= 0:
                        step['expected'][field] = value * scale
            cases.append(case)
    return {'schema_version': 1, 'synthetic_only': True,
            'fixture_sha256': hashlib.sha256(raw).hexdigest(), 'cases': cases}


def evaluate(report, source_sha):
    corpus = generate()
    if not isinstance(source_sha, str) or not re.fullmatch('[0-9a-f]{40}', source_sha):
        raise ValueError('Exact checkout SHA required')
    if (not isinstance(report, dict)
            or set(report) != {'schema_version', 'source_sha', 'fixture_sha256', 'cases'}
            or type(report['schema_version']) is not int or report['schema_version'] != 1
            or report['source_sha'] != source_sha
            or report['fixture_sha256'] != corpus['fixture_sha256']):
        raise ValueError('Malformed, stale or unreviewed Kotlin output')
    expected = {c['id']: c for c in corpus['cases']}
    actual = {}
    if not isinstance(report['cases'], list):
        raise ValueError('Contract cases must be a list')
    for row in report['cases']:
        if (not isinstance(row, dict) or set(row) != {'id', 'results'}
                or not isinstance(row['id'], str) or row['id'] not in expected or row['id'] in actual):
            raise ValueError('Unknown or duplicate contract ID')
        if not isinstance(row['results'], list) or len(row['results']) != len(expected[row['id']]['steps']):
            raise ValueError('Missing history steps')
        actual[row['id']] = row['results']
    if set(actual) != set(expected):
        raise ValueError('Missing financial contracts')
    groups = {g: {'cases': 0, 'failed_cases': 0} for g in sorted(GROUPS)}
    mismatches = []
    for case_id, case in expected.items():
        group = groups[case['group']]
        group['cases'] += 1
        authored = [s['expected'] for s in case['steps']]
        # JSON booleans must not compare equal to integers in financial evidence.
        if json.dumps(actual[case_id], sort_keys=True) != json.dumps(authored, sort_keys=True):
            group['failed_cases'] += 1
            mismatches.append(case_id)
    return {'schema_version': 1, 'source_sha': source_sha, 'synthetic_only': True,
            'production_model_connected': False, 'groups': groups,
            'total_cases': len(expected), 'failures': len(mismatches), 'mismatches': mismatches,
            'scope': 'Pure interpreter/policy contracts; not a database history reconstruction or phone UI test.'}


def main():
    if sys.argv[1:] == ['generate']:
        BUILD.mkdir(parents=True, exist_ok=True)
        (BUILD / 'financial-contracts.json').write_text(json.dumps(generate(), indent=2) + '\n', encoding='utf-8')
        print(f"Generated {len(generate()['cases'])} invented cases across four financial contract groups.")
    elif sys.argv[1:] == ['evaluate']:
        import os
        report = json.loads((BUILD / 'financial-contract-output.json').read_text(encoding='utf-8'))
        result = evaluate(report, os.environ.get('PESATRACK_LAB_SOURCE_SHA'))
        (BUILD / 'financial-contract-evaluation.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
        print(json.dumps(result, indent=2))
        if result['failures']:
            raise SystemExit(1)
    else:
        raise SystemExit('Only generate or evaluate accepted; no external message paths')


if __name__ == '__main__':
    main()
