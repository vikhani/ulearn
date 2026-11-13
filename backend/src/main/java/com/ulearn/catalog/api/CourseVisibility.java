package com.ulearn.catalog.api;

/**
 * Whether a Course's contents can be shared. Defaults to {@link #PRIVATE} so nothing is exposed until
 * the owner explicitly shares it (ADR-0002).
 */
public enum CourseVisibility {
    PRIVATE,
    PUBLIC
}
