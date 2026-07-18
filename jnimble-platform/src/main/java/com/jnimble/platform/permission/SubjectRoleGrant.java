package com.jnimble.platform.permission;

import java.time.Instant;

/**
 * Immutable record representing a role assignment to a subject (user).
 *
 * @param subjectId the subject identifier (typically username)
 * @param roleId    the role identifier
 * @param grantedAt the timestamp when the role was assigned
 */
public record SubjectRoleGrant(String subjectId, String roleId, Instant grantedAt) {
}
