# PickMyRoute Project Analysis: AI Agent Best Practices for Paired Programming
**Analysis Date:** October 6, 2025  
**Project Status:** Functional MVP with package rename in progress  
**Audience:** AI Coding Agents + Human Developers

---

## 🎯 Executive Summary

PickMyRoute was a highly successful AI-assisted paired programming project that delivered a functional navigation app. This analysis codifies patterns that enable **40-60% faster development** by emphasizing:
- **Task-based incremental development** (not time-based)
- **Build validation after each logical unit of work**
- **Proactive planning instead of reactive debugging**

---

## ✅ What Made This Project Successful

### 1. **Crystal Clear Vision from Day One**
The project had a **single, specific problem statement**:
> "Allow drivers to force routes through specific roads they prefer"

**Why this worked:**
- No scope creep discussions
- Every feature decision had clear acceptance criteria
- AI agent could make architectural decisions confidently

**AI Agent Lesson:** Always start by reading/creating `PROJECT.md` with a one-sentence problem statement. Use this as your north star for every decision.

---

### 2. **Clean Slate Approach**
**What was done:** Removed ALL Android template code before building features
**Result:** Zero confusion about active vs legacy code

**AI Agent Lesson:** Before writing any features, propose removing ALL template/scaffold code. A clean baseline eliminates ambiguity about what's intentional vs leftover.

---

### 3. **Domain Models First, API Second**
**What was done:** Defined `Route`, `Waypoint`, `NavigationStep` models based on business needs BEFORE looking at Google Maps API responses

**Result:** When API integration happened, DTOs mapped cleanly to domain models with zero rework

**AI Agent Lesson:** 
1. Ask user about business domain concepts first
2. Define domain models based on business requirements
3. THEN look at external APIs and create adapter layers
4. Never let external API shape dictate your domain model

---

### 4. **Vertical Slice Development**
**What was done:** Built complete features end-to-end (UI → ViewModel → Repository → API) one at a time

**Result:** Integration issues surfaced immediately, not at the end

**AI Agent Lesson:** Avoid horizontal layering (all models → all repos → all UI). Instead:
1. Pick one user-facing feature
2. Build complete stack for that feature
3. Validate it works end-to-end
4. Move to next feature

---

### 5. **Excellent Documentation Discipline**
The project maintained:
- `PROJECT.md` - Vision and architecture
- `LESSONS_LEARNED.md` - Real-time insights
- `docs/setup/` - Step-by-step setup guides
- `docs/api/` - API integration notes
- `docs/development/` - Internal dev notes

**Result:** Context never lost between sessions. New contributors could onboard in minutes.

**AI Agent Lesson:** After each major milestone, update relevant documentation. Treat docs as first-class deliverables, not afterthoughts.

---

## ❌ Critical Mistakes That Cost Development Cycles

### 1. **Didn't Build After Each Logical Unit** ⚠️ **BIGGEST ISSUE**

**What happened:** Wrote extensive code across multiple files without running `./gradlew build` until late

**Cost discovered:**
- Gradle dependency conflicts (Hilt + JavaPoet)
- KSP annotation processing failures  
- Package/directory structure mismatches
- Version catalog syntax errors
- Missing repository declarations

**Development cycles lost:** Multiple rounds of "write → build failure → debug → fix → repeat"

**AI Agent Lesson:** 🔴 **BUILD AFTER EACH LOGICAL UNIT OF WORK**

**What is a "logical unit"?**
- ✅ Created a new class/interface
- ✅ Added a new dependency to version catalog
- ✅ Created a new Hilt module
- ✅ Implemented a repository interface
- ✅ Created ViewModel + State classes
- ✅ Added a new composable function

**Build validation protocol:**
```markdown
1. Write a logical unit of code
2. Immediately run: `./gradlew build`
3. If green → commit and move to next unit
4. If red → fix immediately before continuing
```

---

### 2. **Package Rename Without File Movement**

**What happened:** 
- Changed package declarations in code: `package com.stonecode.pickmyroute`
- But files stayed in old directory: `com/stonecode/mapsroutepicker/`
- Kotlin compiler couldn't find classes
- KSP (Hilt) failed completely

**Why this happened:** AI agents can edit file *content* but can't move files in the filesystem

**Development cycles lost:** Extensive debugging of "class not found" errors

**AI Agent Lesson:** 🔴 **Never edit package declarations manually**

**Correct approach:**
1. Detect when user wants to rename a package
2. **STOP and instruct user:** "I need you to use IDE's 'Refactor → Rename' on the `[package name]` directory. This will move files AND update all references. Let me know when done."
3. Wait for user confirmation
4. Then continue with other changes

---

### 3. **Version Catalog Created Without Validation**

**What happened:** Created `libs.versions.toml` with:
- Missing `[libraries]` section header
- Duplicate key definitions
- Invalid version numbers (e.g., KSP 2.0.21-1.0.29 doesn't exist)
- Plugins listed in wrong section

**Cost:** Build failed with cryptic TOML parsing errors

**AI Agent Lesson:** 🔴 **Validate configuration files immediately after creation**

**Configuration validation protocol:**
```markdown
After creating/editing any config file:
1. libs.versions.toml → Run: `./gradlew help`
2. build.gradle.kts → Run: `./gradlew build`
3. AndroidManifest.xml → Run: `./gradlew assembleDebug`
4. ProGuard rules → Run: `./gradlew assembleRelease`
```

---

### 4. **No Unit Tests Written**

**Current state:** Zero test coverage for business logic

**Risk:** 
- Can't refactor safely
- Regression bugs will happen
- Polyline decoder, route calculation logic untested

**AI Agent Lesson:** 🟡 **Propose tests alongside implementation**

**Test-first protocol:**
```markdown
When implementing business logic:
1. Suggest creating test file first
2. Write 2-3 test cases covering:
   - Happy path
   - Edge case
   - Error case
3. Implement production code
4. Run tests: `./gradlew test`
5. If green → commit both test and implementation
```

---

## 🚀 AI Agent Development Protocol

### Task Planning Phase (Before Writing Code)

#### Step 1: Read Project Context
```markdown
Before any coding session:
1. Read PROJECT.md → Understand vision and current state
2. Read TECH_STACK.md → Know version constraints
3. Run `./gradlew build` → Confirm starting from green state
4. Identify dependencies between tasks
```

#### Step 2: Break Down Feature into Minimal Units
```markdown
For a new feature, decompose into:
1. Domain model (if needed)
2. Repository interface (contract)
3. DTO classes (data mapping)
4. Repository implementation
5. ViewModel + State
6. UI composables

Each is a separate logical unit with its own build validation.
```

#### Step 3: Identify Build Checkpoints
```markdown
Mark explicit validation points:
- [ ] Domain models compile
- [ ] Repository interface compiles
- [ ] DTOs + Retrofit API compiles
- [ ] Repository implementation compiles
- [ ] ViewModel compiles
- [ ] UI compiles
- [ ] App runs on device
```

---

### Incremental Development Protocol

#### For Each Logical Unit:

**1. Write the Minimum Viable Code**
```markdown
- Create 1 class/interface/file
- Keep it focused and single-purpose
- Add only necessary imports
```

**2. Immediate Build Validation**
```bash
./gradlew build
```

**3. Decision Point**
- ✅ **Green:** Commit and describe what was added
- ❌ **Red:** Fix immediately, don't write more code

**4. Move to Next Unit**
```markdown
Only after green build → proceed to next logical unit
```

---

### Dependency Addition Protocol

**Before adding ANY dependency:**

**1. Check Compatibility**
```markdown
Ask yourself:
- What version of Kotlin is the project using?
- What version of Compose?
- What version of Hilt?
- Does this new library conflict with existing versions?
```

**2. Document the Requirement**
```markdown
In your change explanation, note:
"Adding [library] version [X.Y.Z] because [reason]"
"Compatible with existing Kotlin [version], Hilt [version]"
```

**3. Add to Version Catalog**
```toml
[versions]
newLib = "X.Y.Z"

[libraries]
new-lib = { group = "...", name = "...", version.ref = "newLib" }
```

**4. Validate Immediately**
```bash
./gradlew help                    # Validates TOML syntax
./gradlew build --refresh-dependencies
```

**5. Only if Green → Use in Code**
```kotlin
implementation(libs.new.lib)
```

---

### Package/Refactoring Protocol

**When user mentions:**
- "Rename package to..."
- "Move files to..."
- "Refactor package structure..."

**AI Agent Response:**
```markdown
🛑 **STOP - Manual Action Required**

Package renames require IDE refactoring to move files physically.

**Please do this manually:**
1. In Android Studio, right-click on package: `[old.package.name]`
2. Select: Refactor → Rename
3. Enter new name: `[new.package.name]`
4. Check "Search in comments and strings"
5. Click Refactor

This will:
- Move all files to correct directory structure
- Update package declarations
- Update all imports project-wide

**After you've done this, let me know and I'll continue with other changes.**
```

---

### Testing Protocol

**For any business logic code:**

**1. Propose Test-First Approach**
```markdown
"Before implementing [feature], let me create tests to define expected behavior."
```

**2. Create Test File**
```kotlin
// test/kotlin/[package]/[Feature]Test.kt
class FeatureTest {
    @Test
    fun `happy path scenario`() { }
    
    @Test
    fun `edge case scenario`() { }
    
    @Test
    fun `error case scenario`() { }
}
```

**3. Implement Production Code**

**4. Validate**
```bash
./gradlew test
```

**5. If Green → Commit Both**
```bash
git add [test file] [production file]
git commit -m "feat: add [feature] with tests"
```

---

## 📋 AI Agent Session Workflow

### At Session Start:

```markdown
## Pre-Session Context Gathering
1. **Read PROJECT.md**
   - What's the vision?
   - What's already implemented?
   - What's the next priority?

2. **Read TECH_STACK.md**
   - What versions are locked?
   - What are the compatibility constraints?

3. **Check Build Health**
   ```bash
   ./gradlew build
   ```
   - If red → propose fixing build first before new features
   - If green → safe to proceed

4. **Read Last Commit Message**
   - What was the last completed unit?
   - What's the logical next step?

5. **Propose Session Plan**
   "Based on PROJECT.md, I propose implementing [feature] next.
   This breaks down into:
   - [ ] Task 1 (validate: build)
   - [ ] Task 2 (validate: build)
   - [ ] Task 3 (validate: build + device test)
   
   Should I proceed?"
```

---

### During Session (Per Task):

```markdown
## Task Execution Loop

1. **Announce Task**
   "Now implementing: [specific task]"

2. **Write Minimum Code**
   - Create 1 class/file
   - Keep scope tight

3. **Build Validation**
   ```bash
   ./gradlew build
   ```

4. **Handle Result**
   - Green → Commit with clear message
   - Red → Fix before moving on

5. **Progress Update**
   "✅ [Task] complete and validated. Moving to [next task]."
```

---

### At Session End:

```markdown
## Post-Session Documentation

1. **Final Build Check**
   ```bash
   ./gradlew clean build
   ./gradlew test
   ```

2. **Update PROJECT.md**
   - Mark completed features as ✅
   - Update "Current State" section

3. **Update LESSONS_LEARNED.md**
   - Document any challenges encountered
   - Note solutions that worked well

4. **Create TODO.md for Next Session**
   ```markdown
   # Next Session Plan
   
   ## Completed This Session
   - ✅ [Task 1]
   - ✅ [Task 2]
   
   ## Next Priority
   - [ ] [Next logical task]
   - [ ] [Task after that]
   
   ## Build Status
   Last successful build: [timestamp]
   All tests passing: [yes/no]
   ```

5. **Commit All Documentation Updates**
   ```bash
   git add PROJECT.md LESSONS_LEARNED.md TODO.md
   git commit -m "docs: session summary for [feature]"
   ```
```

---

## 🎯 Task Decomposition Examples

### Example 1: "Add Firebase Analytics"

**❌ Wrong Approach (Monolithic):**
```markdown
1. Add Firebase dependencies
2. Create all analytics classes
3. Integrate into ViewModels
4. Test everything
```
*Problem: If build breaks, unclear which change caused it*

**✅ Correct Approach (Incremental):**
```markdown
Task 1: Add Firebase BOM to version catalog
- Add to libs.versions.toml
- Validate: `./gradlew help`
- Commit: "build: add Firebase BOM to version catalog"

Task 2: Add Firebase dependencies to app/build.gradle.kts
- Add firebase-analytics-ktx dependency
- Validate: `./gradlew build --refresh-dependencies`
- Commit: "build: add Firebase Analytics dependency"

Task 3: Create AnalyticsEvents constants object
- Create AnalyticsEvents.kt
- Define event name constants
- Validate: `./gradlew build`
- Commit: "feat: define analytics event constants"

Task 4: Create AnalyticsHelper wrapper
- Create AnalyticsHelper.kt
- Implement logging methods
- Validate: `./gradlew build`
- Commit: "feat: implement analytics helper"

Task 5: Create Hilt module for Firebase
- Create AnalyticsModule.kt
- Provide FirebaseAnalytics instance
- Validate: `./gradlew build`
- Commit: "feat: add analytics Hilt module"

Task 6: Inject into one ViewModel
- Update MapViewModel to inject AnalyticsHelper
- Add one analytics call
- Validate: `./gradlew build`
- Run on device: Verify event in Firebase console
- Commit: "feat: integrate analytics in MapViewModel"
```

*Each task takes 5-10 lines of code and validates independently*

---

### Example 2: "Add Location Tracking"

**Task Breakdown:**
```markdown
Task 1: Add location dependencies to version catalog
→ Validate: `./gradlew help`

Task 2: Add dependencies to app build file
→ Validate: `./gradlew build --refresh-dependencies`

Task 3: Create LocationRepository interface
→ Validate: `./gradlew build`

Task 4: Create LocationRepositoryImpl
→ Validate: `./gradlew build`

Task 5: Create LocationModule for Hilt
→ Validate: `./gradlew build`

Task 6: Add location permission to AndroidManifest
→ Validate: `./gradlew assembleDebug`

Task 7: Create LocationPermissionHandler composable
→ Validate: `./gradlew build`

Task 8: Integrate into MapViewModel
→ Validate: `./gradlew build`

Task 9: Display location on map
→ Validate: `./gradlew installDebug` + run on device

Task 10: Add location tests
→ Validate: `./gradlew test`
```

*Each task is independently buildable and testable*

---

## 🏗️ Project Structure for AI-Assisted Development

### Essential Files for AI Context

```markdown
PROJECT_ROOT/
├── .github/
│   └── copilot-instructions.md      # AI agent coding standards
├── PROJECT.md                        # Vision, features, current state
├── TECH_STACK.md                     # Versions & compatibility matrix
├── LESSONS_LEARNED.md                # Real-time insights
├── TODO.md                           # Next session plan
├── ARCHITECTURE.md                   # System design decisions
└── docs/
    ├── setup/                        # User-facing setup
    ├── api/                          # External API integration notes
    └── development/                  # Internal development notes
```

### What Each File Provides to AI:

**PROJECT.md:**
- Mission statement → Guides feature decisions
- Feature checklist → Shows what's done vs remaining
- Architecture overview → Informs code structure decisions

**TECH_STACK.md:**
- Version constraints → Prevents incompatible dependencies
- Compatibility matrix → Shows tested combinations
- Upgrade path → When libraries can be updated

**LESSONS_LEARNED.md:**
- Patterns that worked → Repeat these
- Patterns that failed → Avoid these
- Project-specific quirks → Remember these

**TODO.md:**
- Next priority → Clear starting point
- Task breakdown → Pre-planned approach
- Blockers → Issues needing human input

---

## 🎓 Key Principles for AI Agents

### 1. **Incremental Over Batch**
```markdown
❌ Write 10 classes → Build once
✅ Write 1 class → Build → Write next class → Build
```

### 2. **Validation Over Assumption**
```markdown
❌ "This should work" → Move on
✅ "Let me verify this works" → `./gradlew build`
```

### 3. **Documentation Over Memory**
```markdown
❌ Remember context in conversation history
✅ Document context in PROJECT.md, read it each session
```

### 4. **Proactive Over Reactive**
```markdown
❌ Wait for build to break → Debug
✅ Validate after each change → Prevent breaks
```

### 5. **Human Guidance Over AI Speculation**
```markdown
❌ Guess at version numbers → Try until it works
✅ Ask human to check Android Studio suggestions
```

### 6. **Test Evidence Over Code Inspection**
```markdown
❌ "This code looks correct"
✅ "Tests pass, therefore correct"
```

---

## 🚫 Anti-Patterns to Avoid

### 1. **Batch Editing Multiple Files Without Build**
```markdown
❌ Edit 5 files → Build → Debug 5 files worth of errors
✅ Edit 1 file → Build → Edit next file
```

### 2. **Adding Dependencies Without Validation**
```markdown
❌ Add to libs.versions.toml → Add to build.gradle → Write code
✅ Add to toml → Validate → Add to gradle → Validate → Use in code
```

### 3. **Assuming Package Changes Are Text-Only**
```markdown
❌ Search/replace package declarations
✅ Stop and ask human to use IDE refactoring
```

### 4. **Implementing Features Without Plan**
```markdown
❌ "I'll add Firebase" → Start coding
✅ "Let me break Firebase integration into 8 tasks" → Execute incrementally
```

### 5. **Deferring Tests to "Later"**
```markdown
❌ "I'll write tests after the feature works"
✅ "Let me write tests that define 'works'"
```

---

## 🎯 Success Metrics for AI Agents

### Build Health
- **Consecutive green builds:** Target > 90%
- **Time in broken state:** Minimize (fix immediately)
- **Build validation frequency:** After every logical unit

### Code Quality
- **Compile errors per session:** Target < 5
- **Test coverage for new code:** Target > 60%
- **Lint warnings introduced:** Target 0

### Planning Accuracy
- **Tasks completed as planned:** Target > 80%
- **Unexpected blockers:** Document in LESSONS_LEARNED.md
- **Estimation accuracy:** Improve over time

### Documentation Quality
- **PROJECT.md freshness:** Update after each session
- **TODO.md accuracy:** Next session should match plan
- **LESSONS_LEARNED.md growth:** Add insights regularly

---

## 💡 AI Agent Self-Check Questions

Before moving to next task, ask:

```markdown
✅ Did I run `./gradlew build` after this change?
✅ Is the build currently green?
✅ Did I commit this logical unit of work?
✅ Is my commit message descriptive?
✅ Did I update PROJECT.md if this completed a feature?
✅ Would a new AI session understand the current state from docs?
```

If any answer is "no" → Address it before continuing.

---

## 🏆 Conclusion

**PickMyRoute succeeded because:**
- Clear vision from PROJECT.md
- Clean architecture decisions
- Excellent documentation discipline
- Good AI-human collaboration

**It could have been 50% faster with:**
- ✅ Build after every logical unit (not batched)
- ✅ Proactive version validation
- ✅ IDE-based refactoring (not text editing)
- ✅ Task decomposition before implementation
- ✅ Test-first development

**For future AI-assisted projects:**
1. Create PROJECT.md, TECH_STACK.md, TODO.md from day one
2. Break every feature into 5-10 minute tasks
3. Validate after each task (build, test, or run)
4. Document learnings in real-time
5. Plan next session before ending current one

**This document IS the template. Copy it, customize it, and use it as the AI agent's operating manual.**

---

**Document Version:** 2.0 - AI Agent Focused  
**Changelog:** Removed time-based intervals, added task decomposition protocols, emphasized incremental validation  
**Next Review:** After next major AI-assisted project
