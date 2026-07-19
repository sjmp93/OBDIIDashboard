# OBDII Dashboard - Plan de Mejora

> Generado: 2026-07-18 | Code review por 10 agentes especializados Kotlin/Android

---

## Resumen Ejecutivo

| Severidad | Count | Descripción |
|-----------|-------|-------------|
| 🔴 Crítico | ~26 | App no funciona / crashes en API 31+ |
| 🟠 Alto | ~40 | Bugs graves + anti-patterns de arquitectura |
| 🟡 Medio | ~58 | Calidad, robustez, APIs deprecated |
| 🟢 Bajo | ~39 | Code style, cleanup, optimization |

**Total: ~163 issues identificados en 9 pantallas + infraestructura core**

---

## FASE 0 — CRÍTICOS (App no funciona / crashes)

### T-0.1: Permisos Bluetooth API 31+ y foreground service
| Afecta | Detalle |
|--------|---------|
| Toda la app, AndroidManifest.xml, ObdService.kt | |

**Problema:** Solo `BLUETOOTH` + `BLUETOOTH_ADMIN` declarados. En Android 12+ (API 31+) esto no es suficiente para scan/connect. Además, el service nunca llama a `startForeground()`, causando kill por el sistema en API 26+.

**Fix:**
- Añadir al manifest:
  ```xml
  <uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
  <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
  ```
- Implementar `startForeground(NOTIFICATION_ID, notification)` en ObdService.onCreate()
- Runtime permission request para BLUETOOTH_SCAN y BLUETOOTH_CONNECT (API 31+)

### T-0.2: BroadcastReceiver nunca unregister + infinite loop BT (StartMenuActivity)
| Afecta | StartMenuActivity.kt |
|--------|----------------------|

**Problema:** Receiver registrado en onStart() sin onStop(). Además, observer de ViewModel llama a `btDevice.enable()/disable()` que dispara broadcast → receiver actualiza ViewModel → loop infinito.

**Fix:**
- Añadir `onStop() { unregisterReceiver(btEventReceiver) }`
- Guard flag anti-reentrada entre observer y receiver

### T-0.3: GlobalScope.launch en DiscoverActivity → memory leak + crash
| Afecta | DiscoverActivity.kt |
|--------|---------------------|

**Problema:** `GlobalScope.launch` no se cancela al destruir Activity. UI updates from background dispatcher causan crash.

**Fix:** Reemplazar todos los `GlobalScope.launch { }` por `lifecycleScope.launch { }`

### T-0.4: TODO("Not yet implemented") throws en onServiceDisconnected
| Afecta | DiscoverActivity.kt, MenuActivityKT.kt |
|--------|---------------------------------------|

**Problema:** `TODO()` lanza `UnsupportedOperationException` cuando el sistema desconecta el service inesperadamente.

**Fix:** Implementar cleanup real: nullificar referencia al servicio y resetear estado UI.

### T-0.5: setContentView() llamada 2 veces en DTC Activity
| Afecta | DiagnosticTroubleCodeActivity.kt |
|--------|----------------------------------|

**Problema:** `fillView()` llama a `setContentView(R.layout.diagnostic_trouble_code_activity)` destruyendo el layout original de `onCreate()`. Botones dejan de funcionar.

**Fix:** Usar View switching (add/remove views) o Fragment en lugar de setContentView segunda vez.

### T-0.6: Sensor listeners nunca unregister (VerboseActivity)
| Afecta | VerboseActivityKT.kt |
|--------|----------------------|

**Problema:** Sensors registrados en onResume() pero nunca unregistered → callbacks firan después de pause, memory leak del Activity.

**Fix:** `sensorManager?.unregisterListener()` para cada listener en onPause().

### T-0.7: LiveData mutation sin nueva referencia (VerboseViewModel)
| Afecta | VerboseViewModel.kt |
|--------|---------------------|

**Problema:** Mutar lista in-place y llamar `postValue(sameReference)` → LiveData no detecta cambio, observers nunca notificados.

**Fix:** Crear nuevo ArrayList en cada update: `obdResultsList.value = ArrayList(receivedDataList)`

### T-0.8: Service unbind missing en onDestroy (ConnectActivity, DTC Activity)
| Afecta | ConnectActivity.kt, DiagnosticTroubleCodeActivity.kt |
|--------|-----------------------------------------------------|

**Problema:** bindService en onCreate/onResume pero no hay cleanup en onDestroy → leak de binder y process.

**Fix:** Añadir `onDestroy() { try { unbindService(serviceConn) } catch (e: Exception) {} }` con flag `isBound`.

---

## FASE 1 — ALTOS (anti-patterns que causan bugs intermitentes)

### T-1.1: runBlocking en PreferencesHelper bloquea main thread
| Afecta | PreferencesHelper.kt, toda la app |
|--------|----------------------------------|

**Problema:** Todos los métodos `*Sync()` usan `runBlocking { ... .first() }` creando un nuevo coroutine scope por llamada y bloqueando el hilo caller. En cold start o storage lento → jank/ANR.

**Fix:** 
- Opción A: Cache in-memory con Flow collection periódica
- Opción B: Exponer suspend functions + llamar desde coroutines existentes
- Mínimo: `withContext(Dispatchers.IO)` en lugar de runBlocking

### T-1.2: runBlocking anidado en ObdService (queueJob + executeQueue)
| Afecta | ObdService.kt |
|--------|---------------|

**Problema:** 
- `queueJob()`: `runBlocking { commandChannel.send(job) }` bloquea main thread si channel buffer full → ANR
- `executeQueue()`: `serviceScope.launch { runBlocking { job.command.run(...) } }` double-dispatch anti-pattern

**Fix:** Hacer `suspend fun queueJob()` o usar `serviceScope.launch`. Eliminar `runBlocking` anidado en executeQueue.

### T-1.3: Activity context leak en Service (todas las activities)
| Afecta | ObdService.kt, DashboardActivity, ConnectActivity, DTC, Verbose |
|--------|------------------------------------------------------------------|

**Problema:** `obdService?.setContext(this@SomeActivity)` pasa referencia strong del Activity a un service long-lived → GC imposible.

**Fix:** Usar `applicationContext` en lugar de Activity context. O WeakReference pattern.

### T-1.4: MutableLiveData expuesto públicamente (6+ ViewModels)
| Afecta | DashboardVM, DiscoverVM, MenuVM, StartVM, VerboseVM, ConnectVM |
|--------|----------------------------------------------------------------|

**Problema:** `var x = MutableLiveData<T>()` público permite que cualquier observer mutee el estado → rompe unidirectional data flow.

**Fix:** Patrón estándar:
```kotlin
private val _x = MutableLiveData<T>()
val x: LiveData<T> = _x
```

### T-1.5: Business logic en Activity (Connect, Chart)
| Afecta | ConnectActivity.kt, ChartActivity.kt |
|--------|--------------------------------------|

**Problema:** 
- Connect: Bluetooth device resolution, MAC validation, socket connection check en Activity
- Chart: `buildChartData()` con regex, float parsing, chart model construction en main thread

**Fix:** Mover lógica a ViewModel. Expose ready-to-render state via LiveData/StateFlow. Offload heavy computation to Dispatchers.Default.

### T-1.6: Service binding lifecycle incorrecto (5 activities)
| Afecta | DashboardActivity, ConnectActivity, DTC Activity, DiscoverActivity, MenuActivityKT |
|--------|-----------------------------------------------------------------------------------|

**Problema:** bindService en onCreate + onResume duplicado. Unbind solo en onPause → leaked bindings.

**Fix:** 
- Bind en onStart(), unbind en onStop() (patrón recomendado)
- Flag `private var isBound = false` para evitar double-bind/unbind

### T-1.7: Thread.sleep() y Dispatchers.Default para I/O blocking (ObdService)
| Afecta | ObdService.kt |
|--------|---------------|

**Problema:** 
- `Thread.sleep(1000)` en startObdConnection() bloquea caller thread
- serviceScope usa Dispatchers.Default pero Bluetooth socket I/O es blocking → starve coroutines

**Fix:** Reemplazar sleep con `delay()` suspend. Cambiar a `Dispatchers.IO`.

### T-1.8: AlarmReceiver fields no persisten entre invocaciones
| Afecta | AlarmReceiver.kt |
|--------|------------------|

**Problema:** Android destruye/recrea BroadcastReceiver instances. Fields `pi` y `am` se pierden → alarm falla silenciosamente.

**Fix:** Inicializar am y rebuild pi dentro de cada onReceive(). O usar singleton/helper class.

### T-1.9: RecyclerView adapter desconectado de ViewModel (Verbose)
| Afecta | VerboseActivityKT.kt, VerboseViewModel.kt |
|--------|-------------------------------------------|

**Problema:** Adapter inicializado con `mutableListOf<ObdDataModel>()` nuevo → datos del ViewModel nunca llegan al RecyclerView.

**Fix:** Observar `viewModel.obdResultsList` y submit results al adapter via DiffUtil/ListAdapter.

### T-1.10: No onRequestPermissionsResult (StartMenuActivity, DiscoverActivity)
| Afecta | StartMenuActivity.kt, DiscoverActivity.kt |
|--------|-------------------------------------------|

**Problema:** Se solicitan permisos pero nunca se verifica el resultado → app procede con permisos denegados y crash downstream.

**Fix:** Override `onRequestPermissionsResult` + handle grants/denials con rationale dialog.

---

## FASE 2 — MEDIOS (calidad, robustez, deprecated APIs)

### T-2.1: Regex compilado en cada iteración → performance
| Afecta | ChartActivity.kt, ConnectActivity.kt, DashboardViewModel.kt |
|--------|-------------------------------------------------------------|

**Problema:** `Regex(...)` creado fresh en cada loop iteration o button click.

**Fix:** `companion object { private val MAC_PATTERN = Regex(...) }` constants.

### T-2.2: Empty catch blocks sin logging
| Afecta | PreferencesHelper.kt, múltiples archivos |
|--------|------------------------------------------|

**Problema:** `catch (_: Exception) {}` → errores invisibles en logs, bugs silentes.

**Fix:** Mínimo `Log.e(TAG, "message", e)` en cada catch block.

### T-2.3: APIs deprecated migración
| Afecta | Múltiples Activities y Service |
|--------|--------------------------------|

**Deprecated → Replacement:**
| API | Reemplazo | Archivos |
|-----|-----------|----------|
| `onBackPressed()` | `OnBackPressedCallback` | Dashboard, Connect, DTC, Menu, Verbose |
| `startActivityForResult()` | Activity Result API | MenuActivityKT, StartMenuActivity |
| `Sensor.TYPE_ORIENTATION` | `getOrientation(accel + magnetometer)` | DashboardActivity, VerboseActivityKT |
| `startService()` / `stopService()` | `ContextCompat.startForegroundService()` | ConnectActivity, DiscoverActivity |

### T-2.4: Unchecked casts en SettingsActivity.onPreferenceChange
| Afecta | SettingsActivity.kt |
|--------|---------------------|

**Problema:** `newValue as Boolean` / `newValue as String` → ClassCastException si preference retorna tipo inesperado.

**Fix:** Safe cast con early return:
```kotlin
val value = newValue as? Boolean ?: return@onPreferenceChange false
```

### T-2.5: Unsafe substring sin length check
| Afecta | DashboardActivity.kt, VerboseActivityKT.kt |
|--------|--------------------------------------------|

**Problema:** `.substring(0, 3)` / `.substring(0, 4)` → IndexOutOfBoundsException si string < length.

**Fix:** `.take(n)` o `if (str.length >= n) str.substring(0, n) else str`

### T-2.6: Magic numbers y strings hardcoded
| Afecta | DashboardViewModel.kt, ChartActivity.kt, MenuViewModel.kt, múltiples |
|--------|----------------------------------------------------------------------|

**Problema:** Valores como `5` (progress bars), `1..5` (menu options), `"OBDDashboard-log"` repetidos sin constante.

**Fix:** Extract to companion object constants / enum class:
```kotlin
enum class MenuOption { DASHBOARD, VERBOSE, CHARTS, DTC, SETTINGS }
const val MAX_PROGRESS_BARS = 5
```

### T-2.7: Settings UI no reactiva + command checkboxes pierden estado rotation
| Afecta | SettingsActivity.kt, SettingsViewModel.kt |
|--------|-------------------------------------------|

**Problema:** 
- LiveData `.value` leído una vez → UI no refleja cambios externos
- CheckBoxPreference creado con `isChecked = true` hardcoded → reset en rotation

**Fix:** 
- `observe(viewLifecycleOwner)` para updates reactivos
- Leer DataStore al crear cada checkbox: `isChecked = PreferencesHelper.getCommandEnabledSync(ctx, cmd.getName())`

### T-2.8: Unsynchronized mutable state multi-thread (ObdService)
| Afecta | ObdService.kt |
|--------|---------------|

**Problema:** `bluetoothSocket`, `bluetoothDevice`, `troubleCodes` escritos desde un thread y leídos desde coroutine IO dispatcher sin sincronización.

**Fix:** `@Volatile` annotations o AtomicReference wrappers.

### T-2.9: e.printStackTrace() → structured logging
| Afecta | ObdService.kt, AlarmReceiver.kt, VerboseActivityKT.kt, múltiples |
|--------|------------------------------------------------------------------|

**Problema:** `printStackTrace()` escribe a stderr sin tag, severity level, ni filtering control.

**Fix:** Reemplazar con `Log.e(TAG, "message", e)` en todas las ocurrencias.

---

## FASE 3 — BAJOS (cleanup)

### T-3.1: Dead code y commented-out código
| Afecta | DashboardActivity.kt, StartMenuActivity.kt, múltiples |
|--------|------------------------------------------------------|

**Fix:** Eliminar dead imports, unused fields, commented blocks, empty lifecycle overrides que solo llaman a super.

### T-3.2: TAG constants duplicados / hardcoded + System.out.println
| Afecta | Múltiples archivos |
|--------|--------------------|

**Problema:** 
- `TAG = "OBD-Log"` duplicado en DiscoverVM y DiscoverActivity
- `System.out.println` en StartMenuActivity (bypass log filtering)
- Empty string log tags: `Log.e("", "msg")`

**Fix:** 
- `companion object { private const val TAG = ClassName::class.simpleName!! }`
- Reemplazar System.out.println con Log.d/i/e
- Tags vacíos → usar TAG constant

### T-3.3: Mutable state exposed + naming conventions
| Afecta | VerboseViewModel.kt, DiscoverViewModel.kt, múltiples |
|--------|-----------------------------------------------------|

**Fix:** 
- `obdReceivedList` → `private val` (VerboseVM)
- Variables PascalCase → camelCase
- Comments en español → English para consistencia

---

## Estimación Temporal Total

| Fase | Issues | Días estimados |
|------|--------|----------------|
| FASE 0 — Críticos | ~26 | 1-2 días |
| FASE 1 — Altos | ~40 | 3-4 días |
| FASE 2 — Medios | ~58 | 2-3 días |
| FASE 3 — Bajos | ~39 | 0.5 días |
| **TOTAL** | **~163** | **~7-10 días** |

---

## Checklist de Verificación Post-Fixes

- [ ] `./gradlew assembleDebug` → BUILD SUCCESSFUL sin errores nuevos
- [ ] Bluetooth scan/connect funciona en emulador API 31+
- [ ] ObdService mantiene conexión como foreground service (notification visible)
- [ ] Rotación de pantalla no pierde estado ni causa crash
- [ ] Todas las activities navegan correctamente entre sí
- [ ] Settings persisten y sobreviven rotation
- [ ] Chart renderiza CSV sin ANR en archivos grandes (>10K rows)
- [ ] DTC screen muestra resultados sin destruir layout original
- [ ] Verbose RecyclerView actualiza con datos OBD en tiempo real
- [ ] No memory leaks detectados (leak canary / profiler)
