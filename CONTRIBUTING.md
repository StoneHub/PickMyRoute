- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] No new warnings
```

## Reporting Bugs

Use GitHub Issues to report bugs. Include:

1. **Description**: Clear description of the bug
2. **Steps to Reproduce**: Exact steps to reproduce the issue
3. **Expected Behavior**: What should happen
4. **Actual Behavior**: What actually happens
5. **Environment**: Device, Android version, app version
6. **Screenshots/Logs**: If applicable

## Feature Requests

We welcome feature requests! Please:

1. Check if the feature already exists or is planned
2. Clearly describe the feature and its benefits
3. Include mockups or examples if helpful
4. Explain your use case

## Development Setup

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11 or later
- Android SDK 24+ (API 34 recommended)

### Configuration

1. Create `local.properties` in project root:
   ```properties
   sdk.dir=/path/to/your/Android/sdk
   MAPS_API_KEY=your_google_maps_api_key
   ```

2. Get a Google Maps API Key from [Google Cloud Console](https://console.cloud.google.com/)
   - Enable: Maps SDK for Android, Directions API, Geocoding API

3. Build the project:
   ```bash
   ./gradlew build
   ```

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## Code of Conduct

### Our Standards

- Be respectful and inclusive
- Welcome newcomers and help them learn
- Accept constructive criticism gracefully
- Focus on what's best for the community
- Show empathy towards others

### Unacceptable Behavior

- Harassment, discrimination, or hate speech
- Trolling, insulting, or derogatory comments
- Publishing others' private information
- Other unprofessional conduct

## Questions?

Feel free to open an issue with the `question` label or reach out to the maintainers.

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
# Contributing to PickMyRoute

Thank you for your interest in contributing to PickMyRoute! This document provides guidelines for contributing to the project.

## Getting Started

1. **Fork the repository** and clone it locally
2. **Create a branch** for your feature or bug fix:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes** following the code style guidelines below
4. **Test your changes** thoroughly
5. **Commit your changes** with clear, descriptive commit messages
6. **Push to your fork** and submit a pull request

## Code Style

This project follows standard Kotlin and Android development practices:

- **Kotlin Style**: Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Formatting**: Use the default Android Studio code formatter
- **Architecture**: MVVM pattern with clean architecture principles
- **Naming**: Use descriptive names that clearly indicate purpose
- **Comments**: Add comments for complex logic, not obvious code

### Code Organization

```
app/src/main/java/com/stonecode/mapsroutepicker/
├── data/          # Data layer (repositories, API, local storage)
├── domain/        # Business logic (models, use cases)
├── ui/            # Presentation layer (Compose UI, ViewModels)
├── di/            # Dependency injection modules
└── util/          # Utility functions and helpers
```

## Pull Request Guidelines

### Before Submitting

- [ ] Code compiles without errors
- [ ] All tests pass (if applicable)
- [ ] No new warnings introduced
- [ ] Code follows project style guidelines
- [ ] Commit messages are clear and descriptive
- [ ] PR description explains what and why

### PR Description Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
How has this been tested?

## Screenshots (if applicable)
Add screenshots for UI changes

## Checklist

