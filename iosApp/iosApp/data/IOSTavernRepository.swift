//
// Created by Oliver Graham on 10/5/25.
//

import Foundation
import FirebaseCore
import FirebaseFirestore
import ComposeApp

// TODO: never name a function with any of these
// [
//     "new...",
//     "alloc...",
//     "init...",
//     "copy...",
//     "mutableCopy...",
//     "get...",
//     "do",
//     "try",
//     "catch",
//     "protocol",
// ]

class IOSTavernRepositoryImplementation : TavernRepositoryCopyForIOS  {

    private let database = Firestore.firestore()
    private let tavernStateFlowContainer: StateFlowContainer<Tavern>

    init(stateFlowContainer: StateFlowContainer<Tavern>) {
        self.tavernStateFlowContainer = stateFlowContainer
    }

    func collectTavernFlow() {
        database.collection(FireStoreKeys.shared.TAVERN)
            .addSnapshotListener { snapshot, error in
                guard let snapshot = snapshot else { return }

                let travelers: [TravelerCodable] = snapshot.documents.compactMap { doc in
                    try? doc.data(as: TravelerCodable.self)
                }

                let tavernCodable = TavernCodable(travelers: travelers)
                self.tavernStateFlowContainer.emit(value: tavernCodable.toTavern())
            }
    }

    func aTravelerEntersTavern(traveler: Traveler) {
        let codableTraveler = traveler.toCodable()

        do {
            try database
                .collection(FireStoreKeys.shared.TAVERN)
                .document(traveler.id)
                .setData(from: codableTraveler, merge: true)
        } catch {
            // TODO: error logging?
        }
    }

    func leaveTavern(id: String) {
        database.collection(FireStoreKeys.shared.TAVERN).document(id).delete()
    }
}
