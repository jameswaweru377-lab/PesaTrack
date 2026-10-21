"""Synthetic query-plan guard for global PDF statement-history pagination.

This host SQLite test checks the index strategy, not Android runtime latency.
No user financial data or existing database file is opened.
"""
import sqlite3
import unittest


class StatementHistoryQueryPlanTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.db.executescript("""
            CREATE TABLE statement_batches(
                id TEXT PRIMARY KEY, account_id TEXT NOT NULL, totals_verified INT NOT NULL
            );
            CREATE TABLE statement_activity_lines(
                batch_id TEXT NOT NULL, line_number INTEGER NOT NULL,
                occurred_at INTEGER NOT NULL, description TEXT NOT NULL,
                paid_in_minor INTEGER NOT NULL, withdrawn_minor INTEGER NOT NULL,
                PRIMARY KEY(batch_id,line_number)
            );
            CREATE INDEX statement_lines_time
                ON statement_activity_lines(batch_id,occurred_at DESC);
            CREATE INDEX statement_lines_recent_global
                ON statement_activity_lines(occurred_at DESC,batch_id,line_number);
        """)
        self.db.executemany(
            "INSERT INTO statement_batches VALUES(?,?,?)",
            [(f"batch_{n}", "synthetic_wallet", 1) for n in range(20)]
        )
        self.db.executemany(
            "INSERT INTO statement_activity_lines VALUES(?,?,?,?,?,?)",
            [
                (
                    f"batch_{n % 20}", n,
                    1_790_000_000_000 - n * 60000,
                    "Payment to Invented Person" if n % 7 == 0 else "Unrelated placeholder",
                    0 if n % 3 else 100,
                    150 if n % 3 else 0
                )
                for n in range(5000)
            ]
        )
        self.query = """
            SELECT s.batch_id, s.line_number FROM statement_activity_lines s
            JOIN statement_batches b ON b.id=s.batch_id
            WHERE (s.paid_in_minor>0 OR s.withdrawn_minor>0)
                AND s.description LIKE ? ESCAPE '!'
            ORDER BY s.occurred_at DESC,s.batch_id,s.line_number LIMIT ? OFFSET ?
        """

    def tearDown(self):
        self.db.close()

    def test_first_page_is_ordered_with_global_index(self):
        args = ("%Invented%Person%", 25, 0)
        plan = self.db.execute("EXPLAIN QUERY PLAN " + self.query, args).fetchall()
        self.assertIn("statement_lines_recent_global", " ".join(row[3] for row in plan))
        rows = self.db.execute(self.query, args).fetchall()
        self.assertEqual(25, len(rows))
        self.assertEqual(("batch_0", 0), rows[0])
        self.assertEqual(("batch_7", 7), rows[1])

    def test_unindexed_and_indexed_queries_return_identical_rows(self):
        args = ("%Invented%Person%", 50, 50)
        with_index = self.db.execute(self.query, args).fetchall()
        self.db.execute("DROP INDEX statement_lines_recent_global")
        without_index = self.db.execute(self.query, args).fetchall()
        self.assertEqual(with_index, without_index)


if __name__ == "__main__":
    unittest.main()
