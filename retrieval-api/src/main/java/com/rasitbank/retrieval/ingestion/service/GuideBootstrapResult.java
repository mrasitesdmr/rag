package com.rasitbank.retrieval.ingestion.service;

/**
 * Rehberin açılıştaki ilk giriş denemesinin sonucudur.
 */
public enum GuideBootstrapResult {
    DISABLED,
    ALREADY_INGESTED,
    JOB_ACTIVE,
    FILE_MISSING,
    JOB_QUEUED
}
