package http.handler

import http.HttpResponse
import http.request.HttpRequest

fun interface RequestHandler {
    suspend fun handle(request: HttpRequest): HttpResponse
}