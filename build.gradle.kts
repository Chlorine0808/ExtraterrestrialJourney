
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.test {
    useJUnitPlatform()
}

fun git(vararg args: String): String =
    String(providers.exec { commandLine("git", *args) }.standardOutput.asBytes.get(), Charsets.UTF_8).trim()

/** The main checkout; the project itself unless this is a worktree. */
fun mainCheckout(): File = file(git("rev-parse", "--path-format=absolute", "--git-common-dir")).parentFile

// A worktree starts without the main checkout's mod configs, so mods fall back to defaults and their IDs clash, and
// without eula.txt, so runServer refuses to start. Copy both from the main checkout before every run; the main
// checkout is the source of truth.
listOf("runClient", "runServer").forEach { name ->
    tasks.named<JavaExec>(name) {
        val main = mainCheckout()
        if (main.canonicalFile != projectDir.canonicalFile) {
            val mainRun = main.resolve(workingDir.relativeTo(projectDir))
            val run = workingDir
            doFirst {
                for (entry in listOf("config", "eula.txt")) {
                    val from = mainRun.resolve(entry)
                    if (from.exists()) from.copyRecursively(run.resolve(entry), overwrite = true)
                }
            }
        }
    }
}

// -PetjShoot=<shot list> starts the End screenshot run. A list missing here is looked up in the main checkout, so
// lists kept in its shots/ work from any worktree. Paths are passed relative to run/client so no non-ASCII
// absolute path lands on the JVM command line. Shots always land in the main checkout's shots/, in a folder
// tagged with the branch.
tasks.named<JavaExec>("runClient") {
    providers.gradleProperty("etjShoot").orNull?.let {
        val main = mainCheckout()
        val list = listOf(file(it), main.resolve(it)).firstOrNull { f -> f.isFile }
            ?: throw GradleException("-PetjShoot: shot list not found: ${file(it)} or ${main.resolve(it)}")
        systemProperty("etj.shoot", list.toRelativeString(workingDir))
        systemProperty("etj.shotsDir", main.resolve("shots").toRelativeString(workingDir))
        systemProperty("etj.shotsBranch", git("rev-parse", "--abbrev-ref", "HEAD"))
        logger.lifecycle("[shoot] list {}, shots go to {}", list, main.resolve("shots"))
        args("--width", "1920", "--height", "1080")
    }
}
