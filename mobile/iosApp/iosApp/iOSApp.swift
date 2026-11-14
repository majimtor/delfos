import SwiftUI

/// The iOS application: a window whose whole content is the Compose Multiplatform module.
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea()
        }
    }
}
