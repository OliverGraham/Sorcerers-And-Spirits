//
//  IOSAuthenticationRepository.swift
//  iosApp
//
//  Created by Oliver Graham on 9/28/25.
//

import Foundation
import FirebaseCore
import FirebaseAuth
import ComposeApp

class IOSAuthenticationRepository: AuthenticationRepository {
    private let authentication = Auth.auth()

    func getUserId() -> String? {
        return authentication.currentUser?.uid
    }

    func isSignedIn() async throws -> KotlinBoolean {
        let signedIn = authentication.currentUser != nil
        return KotlinBoolean(bool: signedIn)
    }

    func signInAnonymously() async throws {
        try await authentication.signInAnonymously()
    }
}
