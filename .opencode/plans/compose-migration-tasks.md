# Tareas de Mejora - OBDDashboard

## Infraestructura

| # | Issue | URL | Estado |
|---|-------|-----|--------|
| 25 | Migrate dependency management to Gradle Version Catalogs (Quip-style) | https://gitlab.erpango.link/mobile/obdii-dashboard/-/work_items/25 | pendiente |

## Bugs UI

| # | Issue | URL | Estado |
|---|-------|-----|--------|
| 26 | Fix DashboardActivity screen cutoff - hardcoded dimensions and broken constraints | https://gitlab.erpango.link/mobile/obdii-dashboard/-/work_items/26 | pendiente |

## Jetpack Compose Migration (Epic + Screens)

| # | Issue | URL | Complejidad | Estado |
|---|-------|-----|-------------|--------|
| 27 | [EPIC] Migrate XML layouts to Jetpack Compose | https://gitlab.erpango.link/mobile/obdii-dashboard/-/work_items/27 | - | pendiente |
| 28 | SettingsActivity (preference screens) | https://gitlab.erpango.link/mobile/obdii-dashboard/-/work_items/28 | baja | pendiente |
| 29 | DiscoverActivity (BT device list) | https://gitlab.erpango.link/mobile/obdii-dashboard/-/work_items/29 | media | pendiente |
| 30 | ConnectActivity (connection UI) | https://gitlab.erpango.link/mobile/obdii-dashboard/-/work_items/30 | baja | pendiente |
| 31 | MenuActivityKT + StartMenuActivity (navigation) | https://gitlab.erpango.link/mobile/obdii-dashboard/-/work_items/31 | media | pendiente |
| 32 | DiagnosticTroubleCodeActivity (DTC codes) | https://gitlab.erpango.link/mobile/obdii-dashboard/-/work_items/32 | media | pendiente |
| 33 | ChartActivity (CSV charts) + chart library replacement | https://gitlab.erpango.link/mobile/obdii-dashboard/-/work_items/33 | alta | pendiente |
| 34 | DashboardActivity (gauges, speedometer, progress bars) | https://gitlab.erpango.link/mobile/obdii-dashboard/-/work_items/34 | muy alta | pendiente |

---

## Orden de Ejecucion Recomendado

### Sprint 1: Fundamentos
1. **#25** - Dependency management (prerequisite for everything)
2. **#26** - Fix dashboard cutoff (fix current UI before replacing it)

### Sprint 2: Compose facil -> medio
3. **#28** - SettingsActivity (practice screen, low risk)
4. **#30** - ConnectActivity  
5. **#29** - DiscoverActivity
6. **#31** - Menu + StartMenu

### Sprint 3: Compose complejo
7. **#32** - DiagnosticTroubleCodeActivity
8. **#33** - ChartActivity (requires Vico library integration)

### Sprint 4: Compose muy complejo
9. **#34** - DashboardActivity (custom Canvas composables for gauges)

---

## Notas Tecnicas

- **Repo GitLab:** https://gitlab.erpango.link/mobile/obdii-dashboard
- **Rama actual:** MVVM-refactor (con fixes criticos mergeados)
- **Referencia para deps:** /Users/sjmp/Developer/Home/Mobile/Quip/gradle/libs.versions.toml

### Como reactivar desde este archivo
Para implementar un issue especifico, decir:
> "Implementa la tarea #XX segun https://gitlab.erpango.link/mobile/obdii-dashboard/-/work_items/XX"
