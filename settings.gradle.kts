pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val ghUser: String = providers.gradleProperty("gpr.user").orNull
    ?: System.getenv("GITHUB_ACTOR") ?: ""
val ghToken: String = providers.gradleProperty("gpr.token").orNull
    ?: System.getenv("GITHUB_TOKEN") ?: ""

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/tribalfs/sesl-androidx")
            credentials { username = ghUser; password = ghToken }
        }
        maven {
            url = uri("https://maven.pkg.github.com/tribalfs/sesl-material-components-android")
            credentials { username = ghUser; password = ghToken }
        }
        maven {
            url = uri("https://maven.pkg.github.com/tribalfs/oneui-design")
            credentials { username = ghUser; password = ghToken }
        }
    }
}

rootProject.name = "NitnemSahib"
include(":app")
