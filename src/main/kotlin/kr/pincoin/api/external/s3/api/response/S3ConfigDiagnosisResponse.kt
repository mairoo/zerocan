package kr.pincoin.api.external.s3.api.response

data class S3ConfigDiagnosisResponse(
    val overallStatus: String, // HEALTHY, ISSUES_FOUND
    val severity: String, // CRITICAL, HIGH, MEDIUM, LOW, NONE
    val issues: List<S3ConfigIssue>,
    val recommendations: List<String>,
    val configSummary: Map<String, Any>
)