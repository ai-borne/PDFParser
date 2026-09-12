package com.payslipmax.pdfparser.billing

/**
 * The RevenueCat public SDK key, safe to embed in client code (RevenueCat's own guidance — same
 * "public, not secret" class as a Firebase apiKey in google-services.json). iOS now ships the real
 * production `appl_...` key (Phase 4 of the monetization rollout); Android still ships the Phase 0
 * Test Store key pending its own rollout.
 */
expect fun revenueCatApiKey(): String
