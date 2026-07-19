# OBDII Dashboard - Work Plan

> Generated: 2026-07-18 | 23 GitLab issues from code review by 10 expert agents  
> Full analysis: [enhancements.md](./enhancements.md)

---

## Issue Tracker

| # | Task ID | Severity | Description | Labels | GitLab Link |
|---|---------|----------|-------------|--------|-------------|
| **FASE 0 — CRÍTICOS** | | | | | |
| 1 | T-0.1 | 🔴 Critical | Permisos Bluetooth API 31+ y foreground service | bluetooth, permissions, foreground-service | [#1](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/1) |
| 2 | T-0.2 | 🔴 Critical | BroadcastReceiver nunca unregister + infinite loop BT (StartMenuActivity) | bluetooth, memory-leak, infinite-loop | [#2](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/2) |
| 3 | T-0.3 | 🔴 Critical | GlobalScope.launch en DiscoverActivity → memory leak + crash UI | coroutines, memory-leak | [#3](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/3) |
| 4 | T-0.4 | 🔴 Critical | TODO() throws en onServiceDisconnected (Discover + Menu) | crash, service-binding | [#4](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/4) |
| 5 | T-0.5 | 🔴 Critical | setContentView() llamada 2 veces en DTC Activity → layout destroy | ui-crash, dtc-screen | [#5](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/5) |
| 6 | T-0.6 | 🔴 Critical | Sensor listeners nunca unregister + LiveData mutation bug (Verbose) | memory-leak, verbose-screen | [#6](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/6) |
| 7 | T-0.7 | 🔴 Critical | Service unbind missing en onDestroy (Connect + DTC Activities) | memory-leak, service-binding | [#7](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/7) |
| **FASE 1 — ALTOS** | | | | | |
| 8 | T-1.1 | 🟠 High | runBlocking en PreferencesHelper bloquea main thread → jank/ANR | coroutines, datastore, performance | [#8](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/8) |
| 9 | T-1.2 | 🟠 High | runBlocking anidado en ObdService (queueJob + executeQueue) → ANR | coroutines, service, performance | [#9](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/9) |
| 10 | T-1.3 | 🟠 High | Activity context leak en Service (todas las activities llaman setContext) | memory-leak, service | [#10](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/10) |
| 11 | T-1.4 | 🟠 High | MutableLiveData expuesto públicamente en 6+ ViewModels → rompe MVVM | mvvm, architecture | [#11](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/11) |
| 12 | T-1.5 | 🟠 High | Business logic en Activity → mover a ViewModel (Connect + Chart) | mvvm, architecture, performance | [#12](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/12) |
| 13 | T-1.6 | 🟠 High | Service binding lifecycle incorrecto en 5 activities → double-bind leaks | service-binding, lifecycle | [#13](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/13) |
| 14 | T-1.7 | 🟠 High | Dispatchers.Default para I/O blocking en ObdService → starve coroutines | coroutines, performance, service | [#14](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/14) |
| 15 | T-1.8 | 🟠 High | AlarmReceiver fields no persisten + intent action genérico | broadcast-receiver, security | [#15](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/15) |
| **FASE 2 — MEDIOS** | | | | | |
| 16 | T-2.1 | 🟡 Medium | Regex compilado en cada iteración → garbage + performance hit | optimization, performance | [#16](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/16) |
| 17 | T-2.2 | 🟡 Medium | Empty catch blocks sin logging → errores invisibles | logging, robustness | [#17](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/17) |
| 18 | T-2.3 | 🟡 Medium | APIs deprecated: onBackPressed, startActivityForResult, TYPE_ORIENTATION, startService | deprecated-api, migration | [#18](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/18) |
| 19 | T-2.4 | 🟡 Medium | Unchecked casts, unsafe substring + unsynchronized state multi-thread | crash-prevention, thread-safety | [#19](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/19) |
| 20 | T-2.5 | 🟡 Medium | Settings UI no reactiva + checkboxes reset rotation + magic numbers | settings-screen, code-quality | [#20](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/20) |
| 21 | T-2.6 | 🟡 Medium | Runtime permissions: no onRequestPermissionsResult + checks innecesarios | permissions, crash-prevention | [#21](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/21) |
| 22 | T-2.7 | 🟡 Medium | RecyclerView adapter desconectado de ViewModel (Verbose screen no muestra datos) | ui-bug, verbose-screen | [#22](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/22) |
| **FASE 3 — BAJOS** | | | | | |
| 23 | T-3.1 | 🟢 Low | Dead code cleanup + unused imports + naming conventions + System.out.println | cleanup, code-quality | [#23](https://gitlab.erpango.link/mobile/obdii-dashboard/-/issues/23) |

---

## Dependencies Graph

```
#1 (BT permissions + foreground service)  ←─ Required by ALL other tasks
   │
   ├── #7 (Service unbind onDestroy) ───────┐
   ├── #10 (Activity context leak) ─────────┤
   └── #13 (Service binding lifecycle) ─────┘
        │
        ├── #2 (BroadcastReceiver + infinite loop)
        ├── #4 (TODO throws on disconnect)
        └── #6 (Sensor unregister, Verbose LiveData)

#8 (PreferencesHelper runBlocking)  ←─ Required by #11, #20
   │
   └── #9 (ObdService runBlocking) ─┘
        │
        ├── #14 (Dispatchers.IO for service)
        └── #15 (AlarmReceiver persistence)

#11 (MutableLiveData encapsulation) ←─ Required by #12
   │
   └── #12 (Business logic → ViewModel)

#21 (Runtime permissions handling)  ←─ Can run in parallel with Fase 0 fixes
```

---

## Recommended Execution Order

### Sprint 1: Critical Fixes (~2 days)
| Day | Tasks | Notes |
|-----|-------|-------|
| D1 | #1, #7, #13 | BT permissions + service lifecycle (unblock everything) |
| D2 | #2, #4, #6, #3 | Receiver leaks, TODO crashes, GlobalScope → lifecycleScope |

### Sprint 2: Architecture (~3 days)  
| Day | Tasks | Notes |
|-----|-------|-------|
| D3 | #8, #9, #14 | Coroutines cleanup (runBlocking removal + Dispatchers.IO) |
| D4 | #10, #11 | Memory leaks + MVVM encapsulation across all VMs |
| D5 | #12, #15 | Business logic migration + AlarmReceiver fix |

### Sprint 3: Quality & Polish (~2 days)
| Day | Tasks | Notes |
|-----|-------|-------|
| D6 | #16-#20 | Regex cache, logging, deprecated APIs, safe casts, settings reactive UI |
| D7 | #21-#23 | Permissions handling, RecyclerView fix, dead code cleanup |

### Sprint 4: Testing (~1 day)
| Day | Tasks | Notes |
|-----|-------|-------|
| D8 | Verification | Run all checklist items from enhancements.md |

---

## Files Affected Summary

| File | Issues touching it | Priority |
|------|-------------------|----------|
| `AndroidManifest.xml` | #1, #15, #21 | Critical + High |
| `service/ObdService.kt` | #1, #9, #10, #14, #17, #19 | Critical + High + Medium |
| `model/PreferencesHelper.kt` | #8, #17, #20 | High + Medium |
| `activity/StartMenuActivity.kt` | #2, #5, #18, #21, #23 | Critical + Medium + Low |
| `activity/DiscoverActivity.kt` | #3, #4, #13, #18, #21 | Critical + High + Medium |
| `activity/DashboardActivity.kt` | #7, #10, #13, #16, #19, #20, #23 | All levels |
| `activity/ConnectActivity.kt` | #7, #12, #13, #18, #16, #23 | High + Medium + Low |
| `activity/DiagnosticTroubleCodeActivity.kt` | #5, #7, #10, #13, #19, #23 | Critical + High + Low |
| `activity/VerboseActivityKT.kt` | #6, #18, #19, #22, #23 | Critical + Medium + Low |
| `vm/DashboardViewModel.kt` | #11, #16, #20 | High + Medium |
| `vm/DiscoverViewModel.kt` | #11 | High |
| `vm/MenuViewModel.kt` | #4, #11, #20 | Critical + High + Medium |
| `vm/VerboseViewModel.kt` | #6, #11, #23 | Critical + High + Low |
| `activity/AlarmReceiver.kt` | #15, #17 | High + Medium |

---

## Post-Fix Verification Checklist

- [ ] `./gradlew assembleDebug --no-daemon` → BUILD SUCCESSFUL  
- [ ] Bluetooth scan/connect funciona en emulador API 31+  
- [ ] ObdService mantiene conexión como foreground service (notification visible)  
- [ ] Rotación de pantalla no pierde estado ni causa crash  
- [ ] Todas las activities navegan correctamente entre sí  
- [ ] Settings persisten y sobreviven rotation  
- [ ] Chart renderiza CSV sin ANR en archivos grandes (>10K rows)  
- [ ] DTC screen muestra resultados sin destruir layout original  
- [ ] Verbose RecyclerView actualiza con datos OBD en tiempo real  
- [ ] No memory leaks detectados (leak canary / profiler)  
