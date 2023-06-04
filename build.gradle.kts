import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.8.20"
    id("org.jetbrains.compose") version "1.4.0"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.common)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.twelvemonkeys:twelvemonkeys:3.9.3")
    implementation("org.imgscalr:imgscalr-lib:4.2")
    implementation("org.zeroturnaround:zt-zip:1.15")
    implementation("org.slf4j:slf4j-nop:2.0.5")
    implementation("ws.schild:jave-core:3.3.1")
    //win
    implementation(compose.desktop.windows_x64)
    implementation("ws.schild:jave-nativebin-win64:3.3.1")
    //linux
    implementation("ws.schild:jave-nativebin-linux-arm64:3.3.1")
    implementation("ws.schild:jave-nativebin-linux64:3.3.1")
    implementation(compose.desktop.linux_x64)
    implementation(compose.desktop.linux_arm64)
    //macos
    implementation("ws.schild:jave-nativebin-osxm1:3.3.1")
    implementation("ws.schild:jave-nativebin-osx64:3.3.1")
    implementation(compose.desktop.macos_arm64)
    implementation(compose.desktop.macos_x64)
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Deb, TargetFormat.Msi)
            packageVersion = "1.7.0"
            packageName = "SICompressor"
            modules("java.instrument", "jdk.unsupported")
            windows {
                menuGroup = "SISoft"
                iconFile.set(project.file("icon.ico"))
            }
        }
    }
}
