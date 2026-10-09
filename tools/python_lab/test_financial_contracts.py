"""Invented policy histories; never a device SMS import interface."""
import copy
import unittest
from tools.python_lab.financial_contracts import generate, evaluate


class FinancialContractsTest(unittest.TestCase):
    def test_four_groups_have_positive_and_negative_cases(self):
        corpus = generate()
        self.assertEqual(corpus, generate())
        self.assertEqual({'ownership', 'transfer', 'debt', 'merchant'}, {c['group'] for c in corpus['cases']})
        self.assertEqual(len(corpus['cases']), len({c['id'] for c in corpus['cases']}))
        self.assertGreaterEqual(len(corpus['cases']), 100)
        self.assertTrue(corpus['synthetic_only'])

    def test_authored_debt_history_does_not_confuse_payment_and_balance(self):
        case = next(c for c in generate()['cases'] if c['id'] == 'debt-known-history-01')
        self.assertEqual([250000, 50000], [s['expected']['amount'] for s in case['steps']])
        self.assertEqual([250000, 200000], [s['expected']['outstanding'] for s in case['steps']])

    def test_no_external_message_inputs(self):
        with self.assertRaises(TypeError):
            generate('my-sms.json')

    def test_evaluation_fails_closed_and_reports_each_group(self):
        corpus = generate()
        rows = [{'id': c['id'], 'results': [s['expected'] for s in c['steps']]} for c in corpus['cases']]
        report = {'schema_version': 1, 'source_sha': 'a' * 40, 'fixture_sha256': corpus['fixture_sha256'], 'cases': rows}
        result = evaluate(report, 'a' * 40)
        self.assertEqual(0, result['failures'])
        self.assertEqual(4, len(result['groups']))
        bad = copy.deepcopy(report)
        bad['cases'][0]['results'][0] = {'match': 'wrong'}
        self.assertEqual(1, evaluate(bad, 'a' * 40)['failures'])
        for mutation in ('stale', 'missing', 'duplicate', 'unknown', 'schema', 'fixture'):
            bad = copy.deepcopy(report)
            if mutation == 'stale': bad['source_sha'] = 'b' * 40
            if mutation == 'missing': bad['cases'].pop()
            if mutation == 'duplicate': bad['cases'].append(bad['cases'][0])
            if mutation == 'unknown': bad['cases'][0]['id'] = 'personal-data'
            if mutation == 'schema': bad['schema_version'] = 2
            if mutation == 'fixture': bad['fixture_sha256'] = '0' * 64
            with self.assertRaises(ValueError, msg=mutation):
                evaluate(bad, 'a' * 40)
