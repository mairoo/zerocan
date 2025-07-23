package kr.pincoin.api.external.s3.api.response

data class S3ConfigIssue(
    val category: String, // CREDENTIALS, BUCKET, REGION, ENDPOINT, TIMEOUT
    val severity: String, // CRITICAL, HIGH, MEDIUM, LOW
    val issue: String,
    val suggestion: String
)