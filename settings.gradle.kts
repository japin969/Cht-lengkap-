pluginManagement {
  repositories {
    maven { url = uri("https://maven.google.com") }
    maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public") }
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    maven { url = uri("https://maven.google.com") }
    maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public") }
  }
}

rootProject.name = "Jayra"

include(":app")
