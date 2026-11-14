import DelfosApp
import SwiftUI
import UIKit

/// Hosts the root view controller of the Kotlin module; Compose draws everything inside it.
struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
