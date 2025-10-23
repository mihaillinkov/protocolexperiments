package http.request

import http.RequestMethod

data class HttpRequestStartLine(
    val url: String,
    val method: RequestMethod)