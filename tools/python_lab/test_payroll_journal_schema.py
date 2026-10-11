import pathlib,re,sqlite3,unittest
ROOT=pathlib.Path(__file__).resolve().parents[2]/'app/src/main/java/com/kedevjimmy/pesatrack/data/local'
class PayrollSchemaTest(unittest.TestCase):
 def setUp(self):
  source=(ROOT/'PayrollJournalSchema.kt').read_text()
  self.sql=[s.strip() for s in re.findall(r'"""(.*?)"""',source,re.S)]
  self.db=sqlite3.connect(':memory:');self.db.execute('PRAGMA foreign_keys=ON')
  self.db.execute('CREATE TABLE transactions(id INTEGER PRIMARY KEY, amount_minor INTEGER, direction TEXT)')
  self.db.execute("INSERT INTO transactions VALUES(1,5200000,'INCOME')")
  self.db.execute('CREATE TABLE money_accounts(id TEXT PRIMARY KEY, balance_minor INTEGER)')
  self.db.execute("INSERT INTO money_accounts VALUES('synthetic-bank',9900000)")
  self.db.commit()
  for sql in self.sql:self.db.execute(sql)
  self.db.commit()
 def record(self,id='p1',key='a'*64):
  self.db.execute('INSERT INTO payroll_records VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',(id,1,'TEST SCHOOL LTD','2026-09',7000000,5200000,'MANUAL',key,'b'*64,1000,1000,1))
 def line(self,pos=0,amount=1800000,record='p1',kind='TAX',role='EMPLOYEE_DEDUCTION'):
  self.db.execute('INSERT INTO payroll_lines VALUES(?,?,?,?,?,?)',(record,pos,'PAYE',kind,role,amount))
 def test_additive_upgrade_and_repeat_creation_preserve_cash(self):
  for sql in self.sql:self.db.execute(sql)
  self.record();self.line();self.db.commit()
  self.assertEqual([(1,5200000,'INCOME')],self.db.execute('SELECT * FROM transactions').fetchall())
  self.assertEqual([('synthetic-bank',9900000)],self.db.execute('SELECT * FROM money_accounts').fetchall())
 def test_replay_key_rejects_second_payroll(self):
  self.record()
  with self.assertRaises(sqlite3.IntegrityError):self.record('p2')
 def test_insertion_failure_rolls_back_header_and_lines(self):
  self.db.execute("CREATE TRIGGER fail_line BEFORE INSERT ON payroll_lines BEGIN SELECT RAISE(ABORT,'synthetic'); END")
  self.db.commit()
  with self.assertRaises(sqlite3.IntegrityError):
   with self.db:self.record();self.line()
  self.assertEqual(0,self.db.execute('SELECT COUNT(*) FROM payroll_records').fetchone()[0])
 def test_delete_culls_only_journal_lines(self):
  self.record();self.line();self.db.execute("DELETE FROM payroll_records WHERE id='p1'")
  self.assertEqual(0,self.db.execute('SELECT COUNT(*) FROM payroll_lines').fetchone()[0])
  self.assertEqual(1,self.db.execute('SELECT COUNT(*) FROM transactions').fetchone()[0])
 def test_orphan_line_rejected_at_commit(self):
  self.line(record='missing')
  with self.assertRaises(sqlite3.IntegrityError):self.db.commit()
  self.db.rollback()
 def test_amount_and_type_constraints(self):
  self.record()
  for amount in [-1,0,1.5]:
   with self.assertRaises(sqlite3.IntegrityError):self.line(amount=amount)
  with self.assertRaises(sqlite3.IntegrityError):self.line(kind='CASH_DEPOSIT')
  with self.assertRaises(sqlite3.IntegrityError):self.line(role='SETTLED')
 def test_backup_round_trip_keeps_reported_lines_separate(self):
  self.record();self.line();self.db.commit()
  restored=sqlite3.connect(':memory:');restored.executescript('\n'.join(self.db.iterdump()))
  self.assertEqual(self.db.execute('SELECT * FROM payroll_lines').fetchall(),restored.execute('SELECT * FROM payroll_lines').fetchall())
  self.assertEqual([(1,5200000,'INCOME')],restored.execute('SELECT * FROM transactions').fetchall())
if __name__=='__main__':unittest.main()

