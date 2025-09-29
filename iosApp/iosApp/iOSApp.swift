import SwiftUI
import FirebaseCore
import ComposeApp

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()

        InitializeKoinKt.initializeKoin(authenticationRepository: IOSAuthenticationRepository())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}