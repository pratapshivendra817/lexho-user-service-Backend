/*
 * Obsolete provider onboarding tables were removed.
 *
 * Keep the canonical partners table intact: the active partner runtime,
 * dispatch, booking, subscription and review flows still use it through
 * PartnerOperationalProfile.
 */
DROP TABLE IF EXISTS provider_service_areas;
DROP TABLE IF EXISTS provider_service_types;
