# Master Documentation Index & Statutory Roadmap (18-Document Architecture)

This master index provides the consolidated architecture, product specifications, deterministic engineering sub-engines, and regulatory compliance blueprints for **ZeroTax-Local**.

---

## Complete 18-Document Master Inventory

| Doc # | Filename | Scope & Primary Focus |
|:---:|---|---|
| **00** | [`00-Master-Index-And-Roadmap.md`](file:///D:/projects/ITR/docs/00-Master-Index-And-Roadmap.md) | Master Phase-by-Phase Roadmap, Form Support (ITR-1/2/4/U), and Document Directory Map |
| **01** | [`01_product_requirements_document.md`](file:///D:/projects/ITR/docs/01_product_requirements_document.md) | Consolidated Product Requirements Document (PRD): Core personas, functional specifications & KPIs |
| **02** | [`02_user_stories_and_acceptance_criteria.md`](file:///D:/projects/ITR/docs/02_user_stories_and_acceptance_criteria.md) | Exhaustive Gherkin BDD Acceptance Criteria for all 4 filing spine stages and pro hub workflows |
| **03** | [`03_information_architecture_and_wireframes.md`](file:///D:/projects/ITR/docs/03_information_architecture_and_wireframes.md) | Screen-by-screen information architecture, ASCII wireframes & multilingual CBDT taxonomy |
| **04** | [`04_technical_architecture_and_system_design.md`](file:///D:/projects/ITR/docs/04_technical_architecture_and_system_design.md) | Multi-module Gradle topology, pure-JVM firewall gate, and production Kotlin sub-engine blueprints |
| **05** | [`05_database_schema_and_on_device_ai_prompts.md`](file:///D:/projects/ITR/docs/05_database_schema_and_on_device_ai_prompts.md) | 13-table SQLCipher 4.5.4 schema, Room DAOs, and on-device AICore Gemini Nano system prompts |
| **06** | [`06_qa_golden_test_vectors_and_security.md`](file:///D:/projects/ITR/docs/06_qa_golden_test_vectors_and_security.md) | Parameterized Golden Tax Calculation Test Vectors (GTV-01..18) and OWASP MASVS security benchmarks |
| **07** | [`07_legal_regulatory_and_compliance.md`](file:///D:/projects/ITR/docs/07_legal_regulatory_and_compliance.md) | Statutory compliance framework: DPDP Act 2023, Income-tax Act 1961, and Aadhaar Act Section 29 |
| **08** | [`08_deterministic_tax_subengines.md`](file:///D:/projects/ITR/docs/08_deterministic_tax_subengines.md) | 14-Gate ITR Form Auto-Router, Rule 2A HRA exemptions, and Section 234A/B/C statutory interest |
| **09** | [`09_on_device_aicore_and_aa_crypto.md`](file:///D:/projects/ITR/docs/09_on_device_aicore_and_aa_crypto.md) | On-Device Gemini Nano wrapper, ReBIT AA X25519 RAM decryptor, and Challan 280 parser |
| **10** | [`10_phase_3_schedule_fa_and_cbdt_json_compiler.md`](file:///D:/projects/ITR/docs/10_phase_3_schedule_fa_and_cbdt_json_compiler.md) | Schedule FA Calendar-Year Engine, Form 67 Rule 128(5) FTC, ITR-U Sec 140B, and CBDT JSON compiler |
| **11** | [`11_jetpack_compose_ui_4_tap_spine_and_modals.md`](file:///D:/projects/ITR/docs/11_jetpack_compose_ui_4_tap_spine_and_modals.md) | Jetpack Compose UI ("4-Tap Spine", Smart Individual PAN keypad, Sec 270A warning dialog & DPDP modal) |
| **12** | [`12_comprehensive_qa_edge_cases_and_unit_tests.md`](file:///D:/projects/ITR/docs/12_comprehensive_qa_edge_cases_and_unit_tests.md) | Comprehensive QA edge-case matrix (ECV-01..06), airplane-mode verification & extended JUnit 5 suite |
| **13** | [`13_production_readiness_checklist_and_ci_pipeline.md`](file:///D:/projects/ITR/docs/13_production_readiness_checklist_and_ci_pipeline.md) | Production release readiness checklist, R8 Full Mode hardening & GitHub Actions CI pipeline |
| **14** | [`Architecture.md`](file:///D:/projects/ITR/docs/Architecture.md) | Master System Architecture: 3-Pillar ingestion layer, pure-JVM firewall, and enterprise animation suite |
| **15** | [`MVP Tech Doc.md`](file:///D:/projects/ITR/docs/MVP%20Tech%20Doc.md) | MVP Technical Specification: CA & Principal Architect edition for salaried & freelance filing |
| **16** | [`PRD.md`](file:///D:/projects/ITR/docs/PRD.md) | Foundational Product Requirements Document: Zero-friction filing UX and on-device privacy moat |
| **17** | [`system Design.md`](file:///D:/projects/ITR/docs/system%20Design.md) | Detailed System Design: Module-by-module data contracts, IPC boundaries, and memory lifecycle |

---

## Master Phase-by-Phase Execution Roadmap

| Phase | Target Release Window | Primary Focus | ITR Forms & Schedules | External API Cost |
|---|---|---|---|---|
| **Phase 1 (MVP)** | Weeks 1 – 6 | Salaried & Freelance Zero-Friction Filing | `ITR-1 (Sahaj)`, `ITR-4 (Sugam - 44ADA/44AD)` | **₹0** (Assisted WebView + On-Device `AICore`) |
| **Phase 2 (Scale)** | Weeks 7 – 12 | Investors, Account Aggregator & UPI Tax Pay | `ITR-2`, `Schedule CG`, `Schedule 112A`, `CYLA/BFLA/CFL`, `Challan 280` | AA per-consent fee (or CAMS PDF local parse at ₹0) |
| **Phase 3 (Moat)** | Weeks 13 – 18 | Foreign RSUs, Vernacular AI Copilot & Revisions | `Schedule FA`, `Form 67`, `Sec 139(5) Revised`, `ITR-U`, `EN/HI/GU` Notice AI | **₹0** (Bundled SBI TTBR DB + On-Device `AICore`) |
