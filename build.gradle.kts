
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.test {
    useJUnitPlatform()
}

// -PetjShoot=<shot list> starts the End screenshot run. The path is passed relative to run/client so no non-ASCII
// absolute path lands on the JVM command line.
tasks.named<JavaExec>("runClient") {
    providers.gradleProperty("etjShoot").orNull?.let {
        systemProperty("etj.shoot", file(it).toRelativeString(workingDir))
        args("--width", "1920", "--height", "1080")
    }
}
