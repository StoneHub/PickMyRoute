# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security issue, please follow these steps:

### Do NOT:
- Open a public GitHub issue
- Disclose the vulnerability publicly before it's been addressed

### Do:
1. **Email**: Send details to [your-email@example.com] with:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Any suggested fixes (if available)

2. **Response Time**: You can expect:
   - Initial response within 48 hours
   - Regular updates on the progress
   - Credit in the security advisory (if desired)

## Security Best Practices for Users

### API Key Protection
- **NEVER** commit `local.properties` to version control
- **NEVER** share your Google Maps API key publicly
- **ALWAYS** restrict your API key in Google Cloud Console:
  - Restrict by application (use your app's package name and SHA-1)
  - Restrict by API (only enable the APIs you use)
  - Set up billing alerts to detect unauthorized usage

### API Key Restrictions Setup
1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Click on your API key
3. Under "Application restrictions", select "Android apps"
4. Add your package name: `com.stonecode.pickmyroute`
5. Add your SHA-1 certificate fingerprint (get it via Android Studio or `keytool`)
6. Under "API restrictions", select "Restrict key"
7. Enable only:
   - Maps SDK for Android
   - Directions API
   - Geocoding API

### Getting Your SHA-1 Fingerprint

**For Debug Builds:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**For Release Builds:**
```bash
keytool -list -v -keystore /path/to/your/keystore.jks -alias your-alias
```

Or use Android Studio:
1. Open Gradle tab (right side)
2. Navigate to: app > Tasks > android > signingReport
3. Copy the SHA1 fingerprint

## Known Security Considerations

### Location Data
- Location data is only used locally on the device
- No location data is sent to third-party servers (except Google Maps APIs)
- Location permissions can be revoked at any time in Android Settings

### Network Communication
- All API calls to Google services use HTTPS
- API keys are stored locally and never transmitted except in authorized API requests

### Third-Party Dependencies
- We regularly update dependencies to patch known vulnerabilities
- Check [build.gradle.kts](app/build.gradle.kts) for current dependency versions

## Security Updates

Security updates will be released as soon as possible after a vulnerability is confirmed. Users will be notified through:
- GitHub Security Advisories
- Release notes
- README updates

## Scope

This security policy applies to:
- The main application code
- Build scripts and configuration
- Documentation that may contain security-relevant information

Out of scope:
- Third-party services (Google Maps API, etc.) - report to the respective vendor
- User device security - follow Android security best practices

