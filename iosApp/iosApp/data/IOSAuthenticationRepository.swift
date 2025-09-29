//
//  IOSAuthenticationRepository.swift
//  iosApp
//
//  Created by Oliver Graham on 9/28/25.
//

import Foundation
import FirebaseCore      // Needed to initialize Firebase
import FirebaseAuth      // For Auth APIs
import ComposeApp // your KMP framework

class IOSAuthenticationRepository: AuthenticationRepository {

    func isSignedIn() async throws -> KotlinBoolean {
        let signedIn = Auth.auth().currentUser != nil
        return KotlinBoolean(bool: signedIn)
    }

    func signInAnonymously() async throws {
        try await Auth.auth().signInAnonymously()
    }
}
