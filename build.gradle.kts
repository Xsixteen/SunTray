plugins {
    java
    application
    eclipse
    idea
}

group = "com.ericulicny"
version = "1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.json:json:20240303")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

application {
    mainClass = "com.ericulicny.suntray.SunTray"
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get()
        )
    }
    // Create a fat JAR with all dependencies
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

// Native macOS app bundling via jpackage
tasks.register<Exec>("jpackage") {
    dependsOn(tasks.jar)
    group = "distribution"
    description = "Creates a native macOS .app bundle using jpackage"

    val jarFile = tasks.jar.get().archiveFile.get().asFile
    val outputDir = layout.buildDirectory.dir("jpackage").get().asFile
    val iconFile = file("src/main/resources/full-moon-icon-md.icns")

    doFirst {
        outputDir.mkdirs()
    }

    commandLine(
        "jpackage",
        "--type", "app-image",
        "--name", "SunTray",
        "--input", jarFile.parentFile.absolutePath,
        "--main-jar", jarFile.name,
        "--main-class", application.mainClass.get(),
        "--dest", outputDir.absolutePath,
        "--icon", iconFile.absolutePath,
        "--app-version", version.toString(),
        "--vendor", "Eric Ulicny Software",
        "--mac-package-identifier", "com.ericulicny.suntray",
        "--java-options", "-Xms128m -Dapple.awt.UIElement=true"
    )

    // Inject LSUIElement into the generated Info.plist so macOS hides the Dock icon
    doLast {
        val plist = File(outputDir, "SunTray.app/Contents/Info.plist")
        if (plist.exists()) {
            val content = plist.readText()
            if (!content.contains("LSUIElement")) {
                val patched = content.replace(
                    "</dict>",
                    "  <key>LSUIElement</key>\n  <true/>\n</dict>"
                )
                plist.writeText(patched)
                logger.lifecycle("Injected LSUIElement into ${plist.absolutePath}")
            }
        }
    }
}
