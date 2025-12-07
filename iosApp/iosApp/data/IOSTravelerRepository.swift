//
// Created by Oliver Graham on 10/5/25.
//

import Foundation
import FirebaseFirestore
import ComposeApp

class IOSTravelerRepository : TravelerRepository {

    private let database = Firestore.firestore()

    func saveTraveler(traveler: Traveler) async throws {
        let codableTraveler = traveler.toCodable()

        try database.collection(FireStoreKeys.shared.TRAVELERS)
            .document(codableTraveler.id)
            .setData(from: codableTraveler, merge: true)
    }

    func getTraveler(id: String) async throws -> Traveler? {
        let snapshot = try await database.collection(FireStoreKeys.shared.TRAVELERS)
            .document(id)
            .getDocument()

        guard let codableTraveler = try? snapshot.data(as: TravelerCodable.self) else { return nil }

        return codableTraveler.toCommon()
    }

    func deleteTraveler(id: String) async throws {
        try await database.collection(FireStoreKeys.shared.TRAVELERS)
            .document(id)
            .delete()
    }

    func updateDisplayName(id: String, displayName: String) async throws {
        try await database.collection(FireStoreKeys.shared.TRAVELERS)
            .document(id)
            .updateData([FireStoreKeys.shared.DISPLAY_NAME: displayName])
    }
}
