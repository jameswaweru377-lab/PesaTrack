# PesaTrack premium notification presentation

Chat owner: CHAT-NOTIFICATIONS

The approved 13 November mockups use the existing cream, coffee-brown and subtle-green theme. Android owns the outside panel, app header, card shape, expand arrow, shade and pop-up behaviour; the app can style the content of supported custom notifications but cannot promise pixel-perfect OEM rendering.

## Grounded content and destinations

- Bills: use the user's confirmed recurring bill category and readable rule label, plus the actual due calendar day. Tapping opens Bills. Never invent payment or due dates.
- Budgets: use existing Plan budget-name, spent amount and limit; display a warning only when the existing policy triggers, and open Plan's budget page.
- Salary: show expected-payday checks without claiming a receipt; a separate received alert is allowed only after a new confirmed ledger salary receipt, and takes the user to Salary.
- Review, goals, money owed, account check and backup: use the existing notification selection and quiet-time policies, preserving accounting uncertainty and no fabricated balances.
- Home: the balance notice remains a persistent, dismissible account check with Add/Update balance and Archive account controls, not a fake bill.
- Previews: eight one-off synthetic samples with a TEST badge. They do not insert financial data. The existing immediate, 1, 10, 20 and 30 minute choices stay intact.

## Release evidence and limits

Android system rules and HyperOS notification settings determine actual banners. The app's custom expanded layout can show the badge, title, details, icon and brown action. Compact heads-up alerts have less space and may be truncated. Lock-screen details are redacted. Device appearance and permission behaviour need a real Redmi test. No emulator or instrumentation run is requested.

The design changes introduce no new financial calculations, database migrations, dependencies or network transmission.
