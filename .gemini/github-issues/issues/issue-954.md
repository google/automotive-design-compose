# Issue #954: Enable DesignModule usage across Gradle projects

## 1. Issue Metadata
- **Issue #:** 954
- **Title:** Enable DesignModule usage across Gradle projects
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/954](https://github.com/google/automotive-design-compose/issues/954)
- **Author:** @Heneveld
- **Created Date:** 2024-04-10T14:48:47Z
- **Updated Date:** 2024-04-10T14:48:47Z
- **Labels:** *None*
- **Priority / Type:** Feature Request / Architecture
- **Status:** Open (Actionable KSP Modularization Enhancement)

---

## 2. Problem Statement & User Impact
In large-scale Android and Automotive applications, teams organize codebases into multi-module Gradle setups (e.g. `:core:designsystem`, `:feature:media`, `:feature:hvac`, `:app`).
DesignCompose introduced `@DesignModuleClass` (via PR #906) to allow modular reuse of customizations (e.g., standard button styling, theme colors, typography).

However, `@DesignModuleClass` currently only works when the module and the `@DesignDoc` consuming it reside in the **same Gradle module / compilation unit**.
If a developer tries to apply `@DesignModule(SharedDesignSystemModule::class)` across a Gradle project boundary (e.g., `implementation(project(":core:designsystem"))`), the KSP code generator fails to discover or compile against the module:
- KSP emits compilation errors or fails to resolve the referenced class symbols.
- Developers cannot build a shared design system library (AAR or Gradle module) consumed across multiple feature modules.
- Teams are forced to duplicate `@DesignDoc` interfaces and customization definitions within each feature module.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt#L134-L195)
  - [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt#L204-L215)

- **Detailed Root Cause:**
  1. In [`BuildModuleClass.kt:134-149`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt#L134-L149):
     ```kotlin
     resolver.getSymbolsWithAnnotation(DesignModuleClass::class.java.name)
     ```
     KSP's `resolver.getSymbolsWithAnnotation()` only inspects symbols in the *current source set being compiled*. Symbols defined in upstream project dependencies (already compiled into `.class` or `.jar`/`.aar` metadata) are not returned by `getSymbolsWithAnnotation` unless classpath scanning or explicit `KSClassDeclaration` resolution is performed.
  2. In [`BuilderProcessor.kt:205`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt#L205), when parsing `@DesignModule(ModuleClass::class)` on a `@DesignComponent`, the processor attempts to look up the module declaration from an internal in-memory map populated strictly during the current KSP round. Since the external module was compiled in a preceding Gradle task, it is absent from the map, causing codegen to abort.

---

## 4. Proposed Fix / Implementation Plan
1. **Fallback to Classpath Resolution via `resolver.getClassDeclarationByName`:**
   In `BuilderProcessor.kt` and `BuildModuleClass.kt`, when resolving `@DesignModule(Target::class)`:
   - If the declaration is not in the active source set, resolve its `KSClassDeclaration` directly from the classpath using:
     ```kotlin
     val classDeclaration = resolver.getClassDeclarationByName(
         resolver.getKSNameFromString(moduleQualifiedName)
     )
     ```
2. **Metadata Serialization / KSP Metadata Inspection:**
   When a module is compiled in an upstream Gradle project, generate a Kotlin metadata marker or inspect the compiled `KSClassDeclaration` methods to extract customization types.
3. **Generate Direct Interface Extension Calls:**
   Ensure generated code references the compiled extension method `ModuleClass.customizations()` without assuming local source generation.

---

## 5. Validation & Reproduction Plan
1. **Multi-project Integration Test:**
   Create a multi-project test harness in `integration-tests/`:
   - `:test-design-system`: Defines `@DesignModuleClass abstract class CoreThemeModule`.
   - `:test-feature-app`: Depends on `:test-design-system` and uses `@DesignModule(CoreThemeModule::class)`.
2. **Execute Build:**
   Run `./gradlew :integration-tests:test-feature-app:assembleDebug` and verify KSP succeeds and generated composables correctly import and call `customizations()`.

---

## 6. Drafted GitHub Comment
```markdown
### Analysis & Proposed Fix for Cross-Project DesignModules:

### Root Cause:
`resolver.getSymbolsWithAnnotation(DesignModuleClass::class.java.name)` in `BuildModuleClass.kt` only queries symbols in the current compilation source set. Upstream dependencies (from other Gradle modules or AARs) are already compiled, so they do not appear in KSP's annotated symbols list.

### Fix Strategy:
1. In `BuilderProcessor.kt`, when resolving `@DesignModule` annotations, look up classes via `resolver.getClassDeclarationByName()` on the classpath if not found in local source declarations.
2. Read the compiled class declaration methods/annotations to reconstruct the customization signature.
3. Wire the generated extension functions to call `customizations()` on the compiled module class.
```
