//
// Created by Oliver Graham on 10/5/25.
//

import Foundation
import ComposeApp

struct TavernCodable : Codable {
    let travelers: [TravelerCodable]
}

extension TavernCodable {
    init(tavern: Tavern) {
        self.travelers = tavern.travelers.map { traveler in
            traveler.toCodable()
        }
    }

    func toTavern() -> Tavern {
        let commonTravelers = self.travelers.map { traveler in
            traveler.toCommon()
        }

        return Tavern(travelers: commonTravelers)
    }
}
