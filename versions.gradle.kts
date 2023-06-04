mapOf(
    "kotlinVersion" to "1.8.21",
    "commonsIO" to "2.6",
    "commonsText" to "1.3"
).forEach { (name, version) ->
    project.extra.set(name, version)
}
