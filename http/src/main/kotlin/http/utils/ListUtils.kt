package http.utils

fun <T> List<T>.split(separator: List<T>): List<List<T>> {
    val res = mutableListOf<List<T>>()

    var current = mutableListOf<T>()

    for (v in this) {
        current.add(v)
        if (current.takeLast(separator.size) == separator) {
            res.add(current.dropLast(separator.size))
            current = mutableListOf()
        }
    }
    if (current.isNotEmpty()) res.add(current)
    return res
}
