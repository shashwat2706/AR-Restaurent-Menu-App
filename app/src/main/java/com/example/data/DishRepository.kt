package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class DishRepository(private val dishDao: DishDao) {

    val allDishes: Flow<List<Dish>> = dishDao.getAllDishes()
    val allCaptures: Flow<List<SavedCapture>> = dishDao.getAllCaptures()

    fun getDishById(id: Int): Flow<Dish?> = dishDao.getDishById(id)

    suspend fun toggleFavorite(id: Int, isCurrentlyFavorite: Boolean) {
        dishDao.updateFavorite(id, !isCurrentlyFavorite)
    }

    suspend fun addCapture(capture: SavedCapture) {
        dishDao.insertCapture(capture)
    }

    suspend fun removeCapture(id: Int) {
        dishDao.deleteCapture(id)
    }

    suspend fun insertDish(dish: Dish) {
        dishDao.insertDishes(listOf(dish))
    }

    suspend fun seedDatabaseIfEmpty() {
        val currentDishes = dishDao.getAllDishes().first()
        if (currentDishes.isEmpty()) {
            val list = listOf(
                Dish(
                    id = 1,
                    name = "Margherita Wood-Fired Pizza",
                    category = "Mains",
                    price = 16.50,
                    description = "Classic Neapolitan style pizza with fresh San Marzano tomatoes, premium buffalo mozzarella, aromatic basil, and extra virgin olive oil.",
                    calories = 820,
                    ingredients = "Wheat Flour, San Marzano Tomatoes, Buffalo Mozzarella, Fresh Basil, Extra Virgin Olive Oil, Sea Salt",
                    allergens = "Gluten, Dairy",
                    prepTime = "12 min",
                    modelColor = 0xFFE57373 // Warm reddish
                ),
                Dish(
                    id = 2,
                    name = "Charred Wagyu Ribeye",
                    category = "Mains",
                    price = 48.00,
                    description = "300g premium A5 Wagyu beef ribeye grilled over oak embers, served with creamy truffle-infused butter, garlic confit, and crispy rosemary potatoes.",
                    calories = 950,
                    ingredients = "Premium Wagyu Beef, Black Truffle Butter, Rosemary, Garlic Confit, Russet Potatoes, Sea Salt, Cracked Black Pepper",
                    allergens = "Dairy",
                    prepTime = "25 min",
                    modelColor = 0xFF8D6E63 // Charcoal brown
                ),
                Dish(
                    id = 3,
                    name = "Avocado Mango Tartare",
                    category = "Appetizers",
                    price = 14.50,
                    description = "Layered fresh Hass avocado, sweet ripe Alphonso mango, and crisp cucumber, finished with a ginger-soy reduction glaze and a sprinkle of toasted sesame seeds.",
                    calories = 320,
                    ingredients = "Hass Avocado, Alphonso Mango, English Cucumber, Ginger-Soy Redux, Citric Dressing, Toasted Sesame, Microgreens",
                    allergens = "Soy, Sesame",
                    prepTime = "8 min",
                    modelColor = 0xFF81C784 // Vibrant green
                ),
                Dish(
                    id = 4,
                    name = "Truffle Mushroom Bruschetta",
                    category = "Appetizers",
                    price = 12.00,
                    description = "Crusty golden sourdough baguette topped with sautéed wild forest mushrooms, cold-pressed white truffle oil, and a spreading of soft whipped artisan ricotta cheese.",
                    calories = 410,
                    ingredients = "Sourdough Baguette, Wild Mushrooms (Porcini & Cremini), Ricotta Cheese, White Truffle Oil, Fresh Thyme, Confit Garlic",
                    allergens = "Gluten, Dairy",
                    prepTime = "10 min",
                    modelColor = 0xFFFFD54F // Golden amber
                ),
                Dish(
                    id = 5,
                    name = "Decadent Lava Fondant",
                    category = "Desserts",
                    price = 11.00,
                    description = "Warm single-origin Belgian dark chocolate cake with a rich molten lava center, paired with a scoop of gourmet Madagascar vanilla bean gelato.",
                    calories = 640,
                    ingredients = "Belgian Chocolate (70% Cacao), Unsalted Butter, Organic Cane Sugar, Cage-free Eggs, Wheat Flour, Vanilla Bean Gelato",
                    allergens = "Gluten, Dairy, Eggs",
                    prepTime = "15 min",
                    modelColor = 0xFF5D4037 // Deep chocolate brown
                ),
                Dish(
                    id = 6,
                    name = "Matcha Pistachio Tiramisu",
                    category = "Desserts",
                    price = 12.50,
                    description = "Delicate ladyfingers soaked in Ceremonial Uji Matcha tea, layered with organic mascarpone sabayon, and finished with roasted Sicilian pistachios.",
                    calories = 530,
                    ingredients = "Uji Matcha Tea, Mascarpone Cheese, Savoiardi Ladyfingers, Fresh Pistachios, Sweet Dairy Cream, Pasteurized Eggs",
                    allergens = "Gluten, Dairy, Nuts, Eggs",
                    prepTime = "10 min",
                    modelColor = 0xFFAED581 // Pale pistachio green
                ),
                Dish(
                    id = 7,
                    name = "Smoked Rosemary Old Fashioned",
                    category = "Drinks",
                    price = 15.00,
                    description = "Small-batch Kentucky bourbon smoked with fresh rosemary sprigs, stirred with Angostura bitters and a candied orange peel garnish.",
                    calories = 180,
                    ingredients = "Kentucky Bourbon, Angostura Bitters, Smoked Rosemary Sprigs, Candied Orange Peel, Organic Demerara Sugar",
                    allergens = "None",
                    prepTime = "5 min",
                    modelColor = 0xFFF57C00 // Deep sunset orange
                ),
                Dish(
                    id = 8,
                    name = "Hibiscus Ginger Elixir",
                    category = "Drinks",
                    price = 8.50,
                    description = "Organic dried Egyptian hibiscus tea leaves cold-brewed and pressed with stinging ginger root, fresh lemon juice, and effervescent sparkling water.",
                    calories = 95,
                    ingredients = "Dried Hibiscus Flowers, Fresh Ginger Root, Cold-pressed Lemon Juice, Organic Agave Nectar, Sparking Mineral Water",
                    allergens = "None",
                    prepTime = "4 min",
                    modelColor = 0xFFE91E63 // Brilliant ruby magenta
                ),
                Dish(
                    id = 9,
                    name = "Badam Mango Delight",
                    category = "Desserts",
                    price = 11.50,
                    description = "A premium blend of sweet Alphonso mangoes and rich almond paste, infused with royal saffron and topped with crushed pistachios and slivered almonds.",
                    calories = 420,
                    ingredients = "Alphonso Mango, Almond Paste, Royal Saffron, Whole Milk, Slivered Pistachios, Cardamom",
                    allergens = "Dairy, Nuts",
                    prepTime = "6 min",
                    modelColor = 0xFFFFB300 // Beautiful golden yellow
                )
            )
            dishDao.insertDishes(list)
        } else if (currentDishes.none { it.id == 9 }) {
            dishDao.insertDishes(listOf(
                Dish(
                    id = 9,
                    name = "Badam Mango Delight",
                    category = "Desserts",
                    price = 11.50,
                    description = "A premium blend of sweet Alphonso mangoes and rich almond paste, infused with royal saffron and topped with crushed pistachios and slivered almonds.",
                    calories = 420,
                    ingredients = "Alphonso Mango, Almond Paste, Royal Saffron, Whole Milk, Slivered Pistachios, Cardamom",
                    allergens = "Dairy, Nuts",
                    prepTime = "6 min",
                    modelColor = 0xFFFFB300 // Beautiful golden yellow
                )
            ))
        }
    }
}
