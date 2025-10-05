# Documentation Guide

Quick reference for finding documentation in this project.

---

## 📁 Documentation Structure

```
docs/
├── setup/                          # Getting started guides
│   └── API_KEY_SETUP.md           # Complete API key setup & troubleshooting
├── api/                           # API reference documentation
│   ├── DIRECTIONS_API.md          # Google Directions API usage
│   ├── GEOCODING_API.md           # Google Geocoding API usage
│   ├── PLACES_API.md              # Google Places API usage
│   └── ROADS_API.md               # Google Roads API usage
└── development/                   # Internal development docs
    ├── LESSONS_LEARNED.md         # Development insights & reflections
    ├── LOGCAT_FILTERING.md        # Debugging tips for logcat
    └── UX_IMPROVEMENTS_ARCHIVE.md # Completed UX work (historical)
```

---

## 🎯 Quick Links by Task

### I want to...

**Set up the project for the first time**
→ Read: `README.md` → `docs/setup/API_KEY_SETUP.md`

**Troubleshoot API key issues**
→ Read: `docs/setup/API_KEY_SETUP.md` (Troubleshooting section)

**Contribute to the project**
→ Read: `CONTRIBUTING.md`

**Understand the Google APIs used**
→ Read: `docs/api/` folder (specific API docs)

**Learn about project decisions**
→ Read: `docs/development/LESSONS_LEARNED.md`

**Debug with logcat**
→ Read: `docs/development/LOGCAT_FILTERING.md`

**Understand security practices**
→ Read: `SECURITY.md` + `docs/setup/API_KEY_SETUP.md` (Security section)

**Report a bug or request a feature**
→ Use GitHub Issues with provided templates

---

## 📚 Root-Level Documentation

- **README.md** - Project overview, quick start
- **CONTRIBUTING.md** - How to contribute
- **SECURITY.md** - Security policy & API key protection
- **LICENSE** - MIT License
- **PROJECT.md** - Detailed project information
- **CLEANUP_CHECKLIST.md** - Ongoing cleanup tasks

---

## 🧹 Recent Changes (October 2025)

### Consolidated Documentation
- ✅ Merged `API_KEY_DEBUGGING.md` + `API_KEY_TROUBLESHOOTING.md` + `GOOGLE_CLOUD_SETUP.md`
  - **Result:** Single comprehensive guide at `docs/setup/API_KEY_SETUP.md`
  
- ✅ Moved internal docs to `docs/development/`
  - `LESSONS_LEARNED.md` - Development insights
  - `LOGCAT_FILTERING.md` - Debugging tips
  - `UX_IMPROVEMENTS_ARCHIVE.md` - Completed UX work

- ✅ Removed redundant files
  - Deleted temporary/duplicate troubleshooting docs
  - Archived completed improvement plans

### Why?
- **User-facing docs** (setup, API reference) separate from **internal docs** (lessons, archives)
- **Single source of truth** for each topic (no duplicate info)
- **Easier to maintain** and find information

---

## 🔍 Finding Information

### Use GitHub's Search
Press `/` in the repository and search for keywords

### Grep for Content
```bash
# Search all docs for a keyword
grep -r "API key" docs/

# Search specific folder
grep -r "Directions" docs/api/
```

### Check Git History
```bash
# See when a doc was last updated
git log --follow docs/setup/API_KEY_SETUP.md
```

---

## ✍️ Contributing to Docs

When adding or updating documentation:

1. **Choose the right location:**
   - User-facing setup → `docs/setup/`
   - API reference → `docs/api/`
   - Internal notes → `docs/development/`
   - General → root level

2. **Update this guide** if you add new categories

3. **Use clear, concise language**

4. **Include examples** (code snippets, commands)

5. **Keep it up to date** - mark docs with "Last Updated" date

---

## 📝 Documentation Standards

- **Format:** Markdown (`.md`)
- **Style:** Clear headings, code blocks, bullet points
- **Structure:** Table of contents for long docs
- **Code:** Use syntax highlighting (```bash, ```kotlin, etc.)
- **Links:** Use relative paths for internal links

---

**Last Updated:** October 2025  
