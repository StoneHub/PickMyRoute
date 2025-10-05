1. Set Expectations Early
    - Assume you’re coding while the user handles Gradle builds, Java/SDK installs,
      and IDE-assisted version upgrades. Ask for the current Kotlin, Compose        
      compiler, Hilt, and AGP versions at the start; confirm whether any overrides  
      (e.g., JavaPoet) already exist.
    - If you need a version bump or confirmation, request the user check Android    
      Studio’s version suggestions or official release notes instead of guessing.   
      Document why the change matters (compatibility, bug fix, etc.).
2. Dependency Hygiene
    - Keep dependency changes minimal and justified. When a processor needs a       
      newer artifact (e.g., Hilt → JavaPoet 1.13+), add a targeted constraint or    
      buildscript dependency and explain the reason. Avoid blanket force rules or   
      downgrades.
    - When touching Compose, ensure composeOptions.kotlinCompilerExtensionVersion   
      matches the version catalog entry; remind the user to confirm compatibility   
      with the Kotlin plugin and Jetpack Compose matrix.
3. Coding Practices
    - Prefer idiomatic Kotlin, clear naming, and small, composable functions.       
      Compose UI code should separate state handling, remember to use State, and    
      hoist state instead of mutating parameters in-place.
    - When working with DI (Hilt), verify scopes and entry points match the intended
      lifecycle; fail early if @InstallIn or module bindings look inconsistent.
    - Add concise comments only when logic isn’t obvious (e.g., non-trivial         
      algorithms or DI wiring). No boilerplate “set X to Y” commentary.
4. Process Discipline
    - Always gather context before editing: inspect relevant files, make a          
      short plan, then write deliberate changes. When diagnosing issues, outline    
      hypotheses before touching code and capture evidence (stack traces, dependency
      chain) via the user.
    - After changes, draft a rollback plan and note manual validation steps the user
      should run (build, tests, dependency insights). If you can’t execute a check  
      yourself, say so and provide the exact command.
5. Communication Guidelines
    - Be explicit about assumptions (e.g., “Hilt 2.57.2 still depends on JavaPoet ≥
      1.13, please confirm the catalog entry”).

- Summaries must include root cause, actions, verification pointers, and next steps.
  Offer short, numbered recommendations so the user can reply with “1” or “2”.

6. When in Doubt
    - If you encounter unfamiliar build errors, ask the user for the full stacktrace
      and dependency insight before modifying dependencies.
    - Defer version-selection choices to the user/Android Studio unless you have    
      documentation confirming the exact version required.
