
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.test {
    useJUnitPlatform()
}

fun git(vararg args: String): String =
    String(providers.exec { commandLine("git", *args) }.standardOutput.asBytes.get(), Charsets.UTF_8).trim()

// -PetjShoot=<shot list> starts the End screenshot run. Paths are passed relative to run/client so no non-ASCII
// absolute path lands on the JVM command line. Shots always land in the main checkout's run/client/etj-shots, even
// from a worktree, in a folder tagged with the branch.
tasks.named<JavaExec>("runClient") {
    providers.gradleProperty("etjShoot").orNull?.let {
        if (!file(it).isFile) throw GradleException("-PetjShoot: shot list not found: ${file(it)}")
        systemProperty("etj.shoot", file(it).toRelativeString(workingDir))
        val main = file(git("rev-parse", "--path-format=absolute", "--git-common-dir")).parentFile
        systemProperty("etj.shotsDir", main.resolve("run/client/etj-shots").toRelativeString(workingDir))
        systemProperty("etj.shotsBranch", git("rev-parse", "--abbrev-ref", "HEAD"))
        logger.lifecycle("[shoot] shots go to {}", main.resolve("run/client/etj-shots"))
        args("--width", "1920", "--height", "1080")
    }
}
