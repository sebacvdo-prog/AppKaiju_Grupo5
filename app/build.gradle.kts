import java.io.File
import java.net.URI
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    jacoco
}

val releaseApiUrl = providers.environmentVariable("KAIJU_RELEASE_API_BASE_URL")
    .orNull?.trim()?.takeIf { it.isNotEmpty() }?.let { url ->
        val uri = runCatching { URI(url) }.getOrNull()
        val host = uri?.host?.lowercase()?.removeSuffix(".")
        require(
            uri != null && uri.scheme.equals("https", ignoreCase = true) && host != null &&
                host !in setOf("localhost", "10.0.2.2", "0.0.0.0", "[::]", "[::1]") &&
                !host.endsWith(".localhost") && !host.startsWith("127.") &&
                (uri.port == -1 || uri.port in 1..65535) &&
                uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null
        ) {
            "KAIJU_RELEASE_API_BASE_URL debe ser una URL HTTPS del backend, sin credenciales, parámetros ni direcciones de loopback."
        }
        uri.toASCIIString().let { if (it.endsWith('/')) it else "$it/" }
    }

val demoTlsCertificate = providers.environmentVariable("KAIJU_DEMO_TLS_CERT")
    .orNull?.takeIf(String::isNotBlank)?.let { path ->
        val certificate = File(path)
        require(certificate.isAbsolute && certificate.isFile) {
            "KAIJU_DEMO_TLS_CERT debe apuntar al certificado público de demostración mediante una ruta absoluta."
        }
        certificate
    }

val releaseSigningVariables = listOf(
    "KAIJU_SIGNING_STORE_FILE",
    "KAIJU_SIGNING_STORE_PASSWORD",
    "KAIJU_SIGNING_KEY_ALIAS",
    "KAIJU_SIGNING_KEY_PASSWORD"
).associateWith { providers.environmentVariable(it).orNull?.takeIf(String::isNotEmpty) }

val releaseStoreFile = if (releaseSigningVariables.values.any { it != null }) {
    val missing = releaseSigningVariables.filterValues { it == null }.keys
    require(missing.isEmpty()) { "Faltan variables de firma release: ${missing.joinToString()}." }
    require(releaseApiUrl != null || demoTlsCertificate != null) {
        "Configura KAIJU_RELEASE_API_BASE_URL o KAIJU_DEMO_TLS_CERT antes de firmar."
    }
    val path = File(releaseSigningVariables.getValue("KAIJU_SIGNING_STORE_FILE")!!)
    require(path.isAbsolute) { "KAIJU_SIGNING_STORE_FILE debe ser una ruta absoluta." }
    val store = path.canonicalFile
    require(store.isFile && !store.toPath().startsWith(rootDir.canonicalFile.toPath())) {
        "El JKS debe existir fuera del repositorio."
    }
    store
} else {
    null
}

android {
    namespace = "com.example.appkaiju_grupo5"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.appkaiju_grupo5"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val kaijuSigning = releaseStoreFile?.let { store ->
        signingConfigs.create("kaijuRelease") {
            storeType = "JKS"
            storeFile = store
            storePassword = releaseSigningVariables.getValue("KAIJU_SIGNING_STORE_PASSWORD")
            keyAlias = releaseSigningVariables.getValue("KAIJU_SIGNING_KEY_ALIAS")
            keyPassword = releaseSigningVariables.getValue("KAIJU_SIGNING_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            releaseApiUrl?.let {
                resValue("string", "kaiju_api_base_url", it)
                signingConfig = kaijuSigning
            }
            optimization {
                enable = false
            }
        }
        create("demoRelease") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            applicationIdSuffix = ".demo"
            signingConfig = kaijuSigning
            resValue("string", "kaiju_api_base_url", "https://localhost:8443/")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        resValues = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}

abstract class PrepararCertificadoDemo : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val certificado: RegularFileProperty

    @get:OutputDirectory
    abstract val recursos: DirectoryProperty

    @TaskAction
    fun exportar() {
        val cert = certificado.get().asFile.inputStream().use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }
        cert.checkValidity()
        require(cert.subjectAlternativeNames.orEmpty().any { it[0] == 2 && it[1] == "localhost" }) {
            "El certificado de demostración requiere SAN DNS:localhost."
        }
        val recurso = recursos.file("raw/kaiju_demo_tls.der").get().asFile
        recurso.parentFile.mkdirs()
        recurso.writeBytes(cert.encoded)
    }
}

androidComponents {
    val demo = selector().withBuildType("demoRelease")
    beforeVariants(demo) { it.enable = demoTlsCertificate != null }
    demoTlsCertificate?.let { certificado ->
        onVariants(demo) { variant ->
            val preparar = tasks.register<PrepararCertificadoDemo>("prepararCertificadoDemo") {
                this.certificado.fileValue(certificado)
            }
            variant.sources.res?.addGeneratedSourceDirectory(preparar, PrepararCertificadoDemo::recursos)
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.exifinterface)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.client)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.withType<Test>().configureEach {
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED",
        "--add-opens=java.base/java.security=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED"
    )
    extensions.configure<JacocoTaskExtension> {
        // Robolectric carga las clases de la app en un classloader aislado sin CodeSource.
        isIncludeNoLocationClasses = true
        includes = listOf("com.example.appkaiju_grupo5.*")
    }
}

val ejecucionDebug = layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")
// Salida de las clases Kotlin de producción de la variante debug (AGP 9).
val logicaDebug = fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
    include(
        "com/example/appkaiju_grupo5/model/**/*.class",
        "com/example/appkaiju_grupo5/viewmodel/**/*.class",
        "com/example/appkaiju_grupo5/data/**/*.class",
        "com/example/appkaiju_grupo5/device/**/*.class",
        "com/example/appkaiju_grupo5/navigation/**/*.class",
        "com/example/appkaiju_grupo5/KaijuApplication*.class",
        "com/example/appkaiju_grupo5/ui/utils/**/*.class",
        "com/example/appkaiju_grupo5/ui/KaijuAppKt*.class",
        "com/example/appkaiju_grupo5/ui/screens/ProductosRouteKt*.class"
    )
    exclude(
        "**/KaijuDatabase_Impl*.class",
        "**/ProductoDao_Impl*.class",
        "**/ComposableSingletons\$*.class"
    )
}
val fuentesDebug = files(layout.projectDirectory.dir("src/main/java"))

tasks.register<JacocoReport>("coberturaLogicaDebug") {
    group = "verification"
    description = "Genera la cobertura de líneas de la lógica Android mediante pruebas JVM."
    dependsOn("testDebugUnitTest")
    executionData(ejecucionDebug)
    classDirectories.from(logicaDebug)
    sourceDirectories.from(fuentesDebug)
    reports {
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/logicaDebug/html"))
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/logicaDebug/cobertura.xml"))
        csv.required.set(false)
    }
}

tasks.register<JacocoCoverageVerification>("verificarCoberturaLogicaDebug") {
    group = "verification"
    description = "Exige al menos 80% de cobertura de líneas en el alcance Android."
    dependsOn("coberturaLogicaDebug")
    executionData(ejecucionDebug)
    classDirectories.from(logicaDebug)
    sourceDirectories.from(fuentesDebug)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
