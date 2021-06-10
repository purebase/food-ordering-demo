package io.axoniq.foodordering.domain

import java.util.*

data class FindFoodCartQuery(val foodCartId: UUID)

class RetrieveProductOptionsQuery