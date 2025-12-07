//
// Created by Oliver Graham on 10/5/25.
//

import Foundation
import ComposeApp

struct TravelerCodable: Codable {
    let id: String
    let displayName: String
}

extension TravelerCodable {
    func toCommon() -> Traveler {
        return Traveler(
            id: self.id,
            displayName: self.displayName
        )
    }
}

extension Traveler {
    func toCodable() -> TravelerCodable {
        return TravelerCodable(
            id: self.id,
            displayName: self.displayName
        )
    }
}
