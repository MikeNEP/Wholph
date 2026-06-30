import java.util.Properties

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Wholphin Extensions (libmpv + ffmpeg/av1 decoders) live in a private
        // GitHub Packages maven repo. Credentials can be provided either via the
        // global/project Gradle properties (e.g. ~/.gradle/gradle.properties) OR
        // via the project's local.properties file (gitignored, safe for secrets).
        val localProperties =
            Properties().apply {
                val localPropertiesFile = rootDir.resolve("local.properties")
                if (localPropertiesFile.exists()) {
                    localPropertiesFile.inputStream().use { load(it) }
                }
            }
        val extensionsUsername =
            providers.gradleProperty("WholphinExtensionsUsername").orNull
                ?: localProperties.getProperty("WholphinExtensionsUsername")
        val extensionsPassword =
            providers.gradleProperty("WholphinExtensionsPassword").orNull
                ?: localProperties.getProperty("WholphinExtensionsPassword")
        if (!extensionsUsername.isNullOrBlank() && !extensionsPassword.isNullOrBlank()) {
            maven("https://maven.pkg.github.com/damontecres/wholphin-extensions") {
                name = "WholphinExtensions"
                credentials {
                    username = extensionsUsername
                    password = extensionsPassword
                }
            }
        }
    }
}

rootProject.name = "Wholphin"
include(":app")
include(":wholphin-mpv-stub")
