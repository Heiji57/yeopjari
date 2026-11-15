pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Pleos Connect SDK 저장소: 플레이그라운드에서 SDK 접근 승인 후 안내받은 주소를 gradle.properties 에 넣는다.
        providers.gradleProperty("yeopjari.pleosRepo").orNull?.takeIf { it.isNotBlank() }?.let { repo ->
            maven { url = uri(repo) }
        }
    }
}

rootProject.name = "yeopjari"
include(":core", ":app")
