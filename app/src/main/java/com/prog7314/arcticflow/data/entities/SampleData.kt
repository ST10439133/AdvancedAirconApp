package com.prog7314.arcticflow.data

import com.prog7314.arcticflow.data.entities.Product

object SampleData {

    fun getSampleProducts(): List<Product> = listOf(
        // Samsung Products
        Product(
            name = "Samsung Inverter 12000 BTU Air Conditioner",
            brand = "Samsung",
            model = "AR12TXHQBWKN",
            btu = 12000,
            price = 12999.99,
            description = "Samsung inverter air conditioner with advanced cooling technology and energy efficiency",
            imagePath = "Samsung/Samsung_Inverter_12000.png",
            brochurePath = "Samsung/Samsung_Inverter_12000.pdf",
            warrantyPath = "Samsung/Samsung_Warranty.pdf",
            rating = 4.5f,
            isFavorite = false
        ),
        Product(
            name = "Samsung AR4500 18000 BTU",
            brand = "Samsung",
            model = "AR18TXHQBWKN",
            btu = 18000,
            price = 15999.99,
            description = "Samsung AR4500 series with powerful cooling and modern design",
            imagePath = "Samsung/Samsung_AR4500_18000.png",
            brochurePath = "Samsung/Samsung_AR4500_18000.pdf",
            warrantyPath = "Samsung/Samsung_Warranty.pdf",
            rating = 4.3f,
            isFavorite = false
        ),

        // LG Products
        Product(
            name = "LG DualCool Inverter 12000 BTU (No Wifi)",
            brand = "LG",
            model = "S12EQ",
            btu = 12000,
            price = 11999.99,
            description = "LG DualCool inverter with dual rotary compressor for efficient cooling",
            imagePath = "LG/LG_DualCool_12000.png",
            brochurePath = "LG/LG_DualCool_12000.pdf",
            warrantyPath = "LG/LG_Warranty.pdf",
            rating = 4.4f,
            isFavorite = false
        ),
        Product(
            name = "LG ArtCool Inverter 12000 BTU (Wifi)",
            brand = "LG",
            model = "S12EW",
            btu = 12000,
            price = 13999.99,
            description = "LG Premium ArtCool inverter with WiFi connectivity and smart features",
            imagePath = "LG/LG_ArtCool_12000.png",
            brochurePath = "LG/LG_ArtCool_12000.pdf",
            warrantyPath = "LG/LG_Warranty.pdf",
            rating = 4.7f,
            isFavorite = false
        ),
        Product(
            name = "LG Cassette Inverter 60000 BTU",
            brand = "LG",
            model = "T60E",
            btu = 60000,
            price = 45999.99,
            description = "LG cassette inverter for commercial spaces with 4-way airflow",
            imagePath = "LG/LG_Cassette_60000.png",
            brochurePath = "LGCassette.pdf",
            warrantyPath = "LG/LG_Warranty.pdf",
            rating = 4.6f,
            isFavorite = false
        ),

        // Carrier Products
        Product(
            name = "Carrier Hi Wall Air Conditioner",
            brand = "Carrier",
            model = "42QH12D",
            btu = 12000,
            price = 9999.99,
            description = "Carrier Hi Wall air conditioner with efficient cooling and reliable performance",
            imagePath = "Carrier/Carrier_HiWall.png",
            brochurePath = "CarrierHiWall.pdf",
            warrantyPath = "Carrier/Carrier_Warranty.pdf",
            rating = 4.2f,
            isFavorite = false
        ),

        // Blu Star Products
        Product(
            name = "Blu Star Cassette Air Conditioner",
            brand = "Blu Star",
            model = "BSC-12C",
            btu = 12000,
            price = 10999.99,
            description = "Blu Star cassette air conditioner with high cooling capacity",
            imagePath = "BluStar/BluStar_Cassette.png",
            brochurePath = "BluStar/BluStar_Cassette.pdf",
            warrantyPath = "BluStar/BluStar_Warranty.pdf",
            rating = 4.0f,
            isFavorite = false
        ),

        // Daikin Products
        Product(
            name = "Daikin VRV Home Catalogue",
            brand = "Daikin",
            model = "VRV-HOME",
            btu = 24000,
            price = 18999.99,
            description = "Daikin VRV Home system with advanced inverter technology",
            imagePath = "Daikin/Daikin_VRV.png",
            brochurePath = "VRV HOME Catalogue.pdf",
            warrantyPath = "Daikin/Daikin_Warranty.pdf",
            rating = 4.8f,
            isFavorite = false
        )
    )
}