plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = libs.versions.compile.sdk.version.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.version.get().toInt()
        namespace = "com.ncorti.kotlin.template.app"
        targetSdk = libs.versions.target.sdk.version.get().toInt()
        applicationId = AppCoordinates.APP_ID
        versionCode = AppCoordinates.APP_VERSION_CODE
        versionName = AppCoordinates.APP_VERSION_NAME
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
    }

    // Use this block to configure different flavors
//    flavorDimensions("version")
//    productFlavors {
//        create("full") {
//            dimension = "version"
//            applicationIdSuffix = ".full"
//        }
//        create("demo") {
//            dimension = "version"
//            applicationIdSuffix = ".demo"
//        }
//    }
}

dependencies {
    implementation(projects.libraryAndroid)
    implementation(projects.libraryCompose)
    implementation(projects.libraryKotlin)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraint.layout)
    implementation(libs.androidx.core.ktx)

    implementation(libs.reflection)
    compileOnly(project(":AndroidStub"))
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
}

afterEvaluate {
    project.android.applicationVariants.forEach { variant ->
        variant.javaCompileProvider.get().doFirst {
            (this as? JavaCompile)?.let {
                val stubProjectPath = project.project(":AndroidStub").projectDir.canonicalPath
                val stubPath = it.classpath.firstOrNull { classPath ->
                    classPath.canonicalPath.startsWith(stubProjectPath)
                }?.canonicalPath ?: throw RuntimeException("reset bootclasspath error")
                it.options.bootstrapClasspath = files(stubPath, it.options.bootstrapClasspath)
            }
        }
    }
}
