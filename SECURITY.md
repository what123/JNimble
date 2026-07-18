# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability within JNimble, please send an email to [178277164@qq.com](mailto:178277164@qq.com). All security vulnerabilities will be promptly addressed.

**Please do NOT report security vulnerabilities through public GitHub issues.**

### What to include

When reporting a vulnerability, please include:

- A description of the vulnerability
- Steps to reproduce the issue
- Potential impact
- Suggested fix (if available)

### Response timeline

- **Acknowledgment**: We will acknowledge receipt of your vulnerability report within 48 hours.
- **Assessment**: We will assess the vulnerability and determine its impact within 5 business days.
- **Fix**: We will work on a fix and aim to release it as soon as possible.
- **Disclosure**: We will coordinate with you on the timing of public disclosure.

## Security Best Practices

When using JNimble in production, we recommend:

1. Keep your JDK and dependencies up to date
2. Use HTTPS in production environments
3. Follow the principle of least privilege for database access
4. Regularly audit plugin permissions
5. Monitor audit logs for suspicious activity

## Dependency Security

We regularly review and update our dependencies to address known vulnerabilities. You can check for security updates by running:

```bash
mvn versions:display-dependency-updates
```

## Contact

For any security concerns, please contact [178277164@qq.com](mailto:178277164@qq.com).
