plugins {
    `java-platform`
    id("com.vanniktech.maven.publish")
}

dependencies {
    constraints {
        api(projects.corbind)
        api(projects.corbindActivity)
        api(projects.corbindAppcompat)
        api(projects.corbindCore)
        api(projects.corbindDrawerlayout)
        api(projects.corbindFragment)
        api(projects.corbindLeanback)
        api(projects.corbindLifecycle)
        api(projects.corbindMaterial)
        api(projects.corbindNavigation)
        api(projects.corbindRecyclerview)
        api(projects.corbindSlidingpanelayout)
        api(projects.corbindSwiperefreshlayout)
        api(projects.corbindViewpager)
        api(projects.corbindViewpager2)
    }
}
