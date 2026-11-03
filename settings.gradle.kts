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
    }
}

rootProject.name = "ZeroTax-Local"

include(":app")
include(":core:model")
include(":core:tax-engine")
include(":core:security")
include(":core:database")
include(":feature:pdf-unlocker")
include(":feature:ai-extractor")
include(":feature:portal-bridge")
include(":feature:aa-aggregator")
