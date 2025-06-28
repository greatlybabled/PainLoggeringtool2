// settings.gradle.kts

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    

}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    
    // Version catalog is already defined in settings.gradle.kts
    // No need to redefine it here
}

rootProject.name = "PainLogger"
include(":app")
include(":Painloggerwatch")
