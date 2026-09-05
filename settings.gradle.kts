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
        maven("https://jitpack.io")
        maven("https://artifact.bytedance.com/repository/pangle/")
        maven("https://android-sdk.is.com")
        maven("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")

    }
}

rootProject.name = "ColorPickerView"
include(":app")
include(":colorpickerview")
include(":colorpickerview-compose")
