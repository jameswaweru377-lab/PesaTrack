# PesaTrack 20-screen reference implementation

This branch implements the approved 20-screen UI direction while preserving PesaTrack's local-first, deterministic financial model. The screenshots are visual references; amounts, accounts, salary receipts, budgets, goals, debts and reports in the app continue to come from the existing stores and message interpretation pipeline rather than illustrative constants.

## Screen mapping

1. **Welcome** — `OnboardingScreen`. Full emerald welcome treatment with PesaTrack lockup, wallet/leaf artwork, privacy line and Get started action.
2. **Connect messages** — smart onboarding SMS-permission stage. Message-reading and background-tracking purpose is explained before Android permission actions.
3. **Import history** — smart onboarding import stage. 1/2/3-month selector, scan explanation and start-fresh option.
4. **Ready** — smart onboarding completion stage. Shows actually detected money sources, salary/live-tracking status and opens the dashboard.
5. **Salary source** — `SalaryScreen` Source tab. Step 1 of 2 with employer, receiving account and payday/reminder setup.
6. **Salary receipt** — `SalaryScreen` Add salary tab. Step 2 of 2 with real amount and calendar receipt date.
7. **Salary saved** — `SalaryScreen` Overview. Success state, latest real receipt, pay-cycle status, history and safe removal.
8. **Profile** — live receipt-coverage money picture, account/goal counts and connected settings/account/data-health rows.
9. **Home** — tracked-funds hero, real money-in/spent metrics, Add salary/Bills/Goals/Reports actions and existing connected money picture/activity cards.
10. **Activity** — real ledger, search, month/pay-cycle controls, trusted summaries, filters and grouped transaction rows.
11. **Pay cycle** — real cycle surplus, money in/spent/saved/invested metrics, fees/transfers and salary-to-salary navigation.
12. **Monthly plan** — `PlanScreen` with current-cycle hero, budgets, goals, commitments and cycle accounting.
13. **Reports & insights** — new connected Week/Month/Year report route using trusted Activity totals and category breakdowns.
14. **Home dark** — same live Home hierarchy rendered through the emerald dark theme.
15. **Accounts dark** — same connected Accounts screen rendered through the emerald dark theme.
16. **Plan dark** — same connected Plan screen rendered through the emerald dark theme.
17. **Accounts light** — confirmed accounts and evidence-based balances with a spending-accounts hero.
18. **M-Pesa/account details** — generic connected account-detail route; M-Pesa gets wallet artwork and all accounts use real balance evidence and linked activity.
19. **Saving goals** — connected savings-target route backed by savings smart rules and actual savings activity.
20. **Bills & reminders** — connected commitment route backed by the current pay-cycle commitment model.

## Navigation

The main bottom tabs remain Home, Activity, Plan, Accounts and More. The concepts are connected without replacing the product architecture:

- Home quick actions: Salary, Bills, Goals, Reports.
- Plan: Budget and Saving goals.
- Accounts: tap a confirmed account to open account details; Debt remains available.
- More: Reports & insights, Saving goals, Bills & reminders, Import history, Data Health, Profile and Settings.
- The legacy `goals` route redirects to the connected Saving goals screen.

## Data integrity rules kept during the UI pass

- No screenshot amount, account number, employer, balance or goal is hard-coded as user data.
- Missing balance evidence is displayed as unknown/waiting rather than guessed.
- Salary is recorded only from a real captured payment or an explicit manual receipt.
- Pay-cycle boundaries continue to be rebuilt from salary receipt evidence.
- Needs-review and unresolved duplicate items stay outside trusted totals.
- Reports use the canonical Activity snapshot logic.
- Bills use pay-cycle commitments and matching activity.
- Savings goals use configured savings rules plus observed savings activity.
- Financial message processing remains local-first; the UI pass does not change the provider-neutral interpretation architecture.

## Visual assets

The wallet, leaf and supporting emerald artwork are implemented as Compose components in `ReferenceConceptComponents.kt`. This keeps the asset scalable across Redmi-class screens, supports light/dark variants, and avoids coupling financial screens to static mock screenshot data.
