@echo off
echo Deleting redundant documentation files...

del "docs\API_KEY_DEBUGGING.md"
del "docs\API_KEY_TROUBLESHOOTING.md"
del "docs\GOOGLE_CLOUD_SETUP.md"
del "docs\LESSONS_LEARNED.md"
del "docs\LOGCAT_FILTERING.md"
del "docs\UX_IMPROVEMENTS.md"
del "docs\UX_IMPROVEMENTS_COMPLETE.md"
del "docs\UX_IMPROVEMENT_PLAN.md"
del "docs\CODE_CLEANUP_SUMMARY.md"

echo Done! 9 files deleted.
echo.
echo New documentation structure:
echo   docs/README.md - Navigation guide
echo   docs/setup/API_KEY_SETUP.md - Consolidated setup guide
echo   docs/api/ - API references (unchanged)
echo   docs/development/ - Internal docs (moved/archived)

