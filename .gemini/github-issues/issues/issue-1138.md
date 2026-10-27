# Issue #1138: Custom Design Modules must be in same directory as Generated Composable

## 1. Issue Metadata
- **Issue #:** 1138
- **Title:** Custom Design Modules must be in same directory as Generated Composable
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/1138](https://github.com/google/automotive-design-compose/issues/1138)
- **Author:** @Heneveld
- **Created Date:** 2024-06-11T13:46:27Z
- **Updated Date:** 2024-06-11T13:46:27Z
- **Labels:** `bug`
- **Priority / Type:** Bug / Code Generation
- **Status:** Open (Actionable KSP Compiler Bug)

---

## 2. Problem Statement & User Impact
When organizing Kotlin projects, developers follow standard package structures (e.g. `com.example.app.ui.modules` for reusable design modules and `com.example.app.ui.screens` for `@DesignDoc` screens).

However, when a `@DesignModuleClass` is declared in a different package from the `@DesignDoc` composable:
- The generated Kotlin file fails to compile with an unresolved reference error:
  `Unresolved reference: customizations`.
- Developers are forced to place all module classes in the exact same package/directory as the generated composables, preventing clean architecture and modular code organization.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt#L795-L810)
  - [`codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt#L180-L195)

- **Detailed Root Cause:**
  1. In [`BuildModuleClass.kt:185`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuildModuleClass.kt#L185), the KSP generator generates an extension function `customizations()` inside the package of the module class:
     ```kotlin
     package com.example.app.ui.modules
     fun MyModule.customizations(): CustomizationContext { /* returns context */ }
     ```
  2. In [`BuilderProcessor.kt:799`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt#L799), when generating the composable function in `com.example.app.ui.screens`, the generator invokes:
     ```kotlin
     customizations.mergeFrom($value.customizations())
     ```
  3. However, `BuilderProcessor.kt` fails to emit an `import com.example.app.ui.modules.customizations` statement into the generated file's import block!
  4. If both classes are in the same package, Kotlin does not require an import, so it compiles. As soon as the packages differ, the compiler cannot find `.customizations()` and compilation fails.

---

## 4. Proposed Fix / Implementation Plan
1. **Emit Missing Extension Import:**
   In [`BuilderProcessor.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt) around line 799:
   When collecting imports for the generated `@DesignDoc` file, iterate through all referenced `@DesignModule` classes. For each module class whose package differs from the target composable package, add an explicit import:
   ```kotlin
   val modulePackage = moduleClassDeclaration.packageName.asString()
   if (modulePackage != targetPackage) {
       fileSpecBuilder.addImport(modulePackage, "customizations")
   }
   ```
2. **Or Fully Qualify Extension Function Call:**
   Alternatively, invoke `com.android.designcompose.codegen.getCustomizations($value)` or qualify the call.

---

## 5. Validation & Reproduction Plan
1. **Reproduction Test:**
   In `codegen/src/test/kotlin/`, create a test case:
   - Module in `package com.test.module`
   - Composable in `package com.test.screen`
2. **Run KSP Test:**
   Execute `./gradlew :codegen:test` and verify that the generated code contains `import com.test.module.customizations` and compiles without errors.

---

## 6. Drafted GitHub Comment
```markdown
### Analysis & Fix for Issue #1138:

### Root Cause:
`BuildModuleClass.kt` generates the `MyModule.customizations()` extension function inside the module's package. When `BuilderProcessor.kt` generates composable files in a different package, it invokes `customizations.mergeFrom(module.customizations())` without importing the extension function.

### Fix:
In `BuilderProcessor.kt`, add an import statement for `<modulePackage>.customizations` whenever the module's package does not match the generated file's package.
```
