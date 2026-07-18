# Contributing to JNimble

Thank you for your interest in contributing to JNimble! This document provides guidelines and instructions for contributing.

## Code of Conduct

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before contributing. We expect all contributors to follow it.

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Gitee Issues](https://gitee.com/what520/jnimble/issues).
2. If not, create a new issue with:
   - A clear, descriptive title
   - Steps to reproduce the problem
   - Expected behavior
   - Actual behavior
   - Environment details (JDK version, OS, etc.)

### Suggesting Features

1. Check existing issues for similar suggestions.
2. Create a new issue with the `feature-request` label.
3. Clearly describe the feature and its use case.

### Pull Requests

1. Fork the repository.
2. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Make your changes following the coding standards.
4. Write or update tests as needed.
5. Update documentation if your changes affect public APIs.
6. Commit your changes with a clear commit message.
7. Push to your fork and submit a pull request.

## Development Setup

### Prerequisites

- JDK 21
- Maven 3.9+

### Building the Project

```bash
mvn clean install
```

### Running Tests

```bash
mvn test
```

### Running the Application

```bash
mvn -pl jnimble-starter spring-boot:run
```

## Coding Standards

### Java Code Style

- Follow standard Java coding conventions.
- Use meaningful variable and method names.
- Add Javadoc comments for public APIs.
- Keep methods focused and reasonably short.

### Commit Messages

- Use the present tense ("Add feature" not "Added feature").
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...").
- Keep the first line under 72 characters.
- Reference issues and pull requests in the body when relevant.

Example:
```
Add plugin hot-reload endpoint

- Implement POST /api/plugins/{id}/reload
- Add tests for the new endpoint
- Update plugin development guide

Closes #42
```

## Pull Request Process

1. Ensure your code compiles and passes all tests.
2. Update the README.md if needed.
3. Add entries to CHANGELOG.md for notable changes.
4. Request a review from maintainers.
5. Address review feedback promptly.

## Reporting Security Vulnerabilities

Please see [SECURITY.md](SECURITY.md) for instructions on reporting security vulnerabilities.

## Questions?

If you have questions about contributing, feel free to open an issue with the `question` label.

Thank you for contributing to JNimble!
