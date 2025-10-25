package http.request

data class HttpRequestStartLine(
    val url: String,
    val method: RequestMethod)