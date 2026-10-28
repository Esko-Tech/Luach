/*
 * Luach, a Kotlin Multiplatform library for the Hebrew calendar and zmanim.
 * Copyright (C) 2026 Esko Tech Inc.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7: this program may
 * be conveyed through Apple's application distribution services as set out
 * in ADDITIONAL-PERMISSIONS.md at the root of the repository.
 */

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.binaryCompatibilityValidator)
}

group = "com.eskotech"
version = "0.1.0"

kotlin {
    jvmToolchain(21)

    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    macosArm64()
    macosX64()
    linuxX64()
    linuxArm64()
    mingwX64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.named<Test>("jvmTest") {
    if (providers.gradleProperty("updateSnapshots").isPresent) {
        systemProperty("snapshots.update", file("src/jvmTest/resources/snapshots").absolutePath)
        outputs.upToDateWhen { false }
        outputs.cacheIf { false }
    }
    systemProperty("luach.version", project.version.toString())
}

apiValidation {
    ignoredProjects.add("sample")
}

mavenPublishing {
    coordinates(group.toString(), "luach", version.toString())
    configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty(), sourcesJar = true))
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    pom {
        name.set("Luach")
        description.set(
            "A Kotlin Multiplatform library for astronomical and halachic times (zmanim), the Jewish " +
                "calendar, parsha, Daf Yomi and tefila rules.",
        )
        url.set("https://github.com/Esko-Tech/Luach")
        licenses {
            license {
                name.set("GNU General Public License v3.0 only")
                url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("eskotech")
                name.set("Esko Tech Inc.")
            }
        }
        scm {
            url.set("https://github.com/Esko-Tech/Luach")
            connection.set("scm:git:https://github.com/Esko-Tech/Luach.git")
            developerConnection.set("scm:git:ssh://git@github.com/Esko-Tech/Luach.git")
        }
    }
}
