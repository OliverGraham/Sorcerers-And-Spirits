//
// Created by Oliver Graham on 10/5/25.
//

import Foundation
import FirebaseCore
import FirebaseFirestore
import FirebaseDatabase
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

    private let database = Database.database()
    private let tavernReference: DatabaseReference
    private let tavernStateFlowContainer: StateFlowContainer<Tavern>
    private var travelersInTavern: [Traveler] = []

    init(stateFlowContainer: StateFlowContainer<Tavern>) {
        self.tavernStateFlowContainer = stateFlowContainer
        self.tavernReference = database.reference(withPath: FireStoreKeys.shared.TAVERN)
    }

    func getTavernStream() {
        tavernReference.observe(.childAdded) { [weak self] snapshot in
            guard let self = self else { return }

            guard let dict = snapshot.value as? [String: Any] else { return }

            do {
                let data = try JSONSerialization.data(withJSONObject: dict)
                let travelerCodable = try JSONDecoder().decode(TravelerCodable.self, from: data)

                guard let traveler = travelerCodable.toCommon() as Traveler? else { return }

                self.travelersInTavern.append(traveler)
                let tavern = Tavern(travelers: self.travelersInTavern)
                self.tavernStateFlowContainer.emit(value: tavern)
            } catch {
                // TODO: error logging?
            }
        }
    }


    func collectTavernFlow() {
        observeChild(.childAdded) { self.travelersInTavern.append($0) }
        observeChild(.childRemoved) { traveler in
            self.travelersInTavern.removeAll { $0.id == traveler.id }
        }
    }


    private func observeChild(_ event: DataEventType, modifyList: @escaping (Traveler) -> Void) {
        tavernReference.observe(event) { [weak self] snapshot in
            guard let self = self else { return }
            guard let dict = snapshot.value as? [String: Any] else { return }

            do {
                let data = try JSONSerialization.data(withJSONObject: dict)
                let travelerCodable = try JSONDecoder().decode(TravelerCodable.self, from: data)

                guard let traveler = travelerCodable.toCommon() as Traveler? else { return }

                // Modify the travelersInTavern array according to the closure
                modifyList(traveler)

                // Emit updated Tavern
                let tavern = Tavern(travelers: self.travelersInTavern)
                self.tavernStateFlowContainer.emit(value: tavern)

            } catch {
                print("Failed to decode traveler: \(error)")
            }
        }
    }

    func aTravelerEntersTavern(traveler: Traveler) {
        let codableTraveler = traveler.toCodable()

        do {
            let data = try JSONEncoder().encode(codableTraveler)
            let dict = try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]

            let travelerRef = tavernReference.child(traveler.id)
            travelerRef.setValue(dict)                  // Add traveler to tavern
            travelerRef.onDisconnectRemoveValue()       // Remove automatically on disconnect
        } catch {
            // TODO: error logging?
        }
    }
}
