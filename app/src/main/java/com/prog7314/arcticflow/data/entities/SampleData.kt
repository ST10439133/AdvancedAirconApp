package com.prog7314.arcticflow.data

import com.prog7314.arcticflow.data.entities.Product

object SampleData {

    fun getSampleProducts(): List<Product> = listOf(

        // ==================== Samsung ====================
        Product(
            name = "Samsung Inverter 12000 BTU Air Conditioner",
            brand = "Samsung",
            model = "AR12TXHQBWKN",
            btu = 12000,
            price = 12999.99,
            description = "Samsung inverter air conditioner with advanced cooling technology and energy efficiency",
            imagePath = "Samsung_Inverter_12000.png",
            brochurePath = "Samsung_Inverter_12000.pdf",
            warrantyPath = "Samsung_Warranty.pdf",
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
            imagePath = "Samsung_AR4500_18000.png",
            brochurePath = "Samsung_AR4500_18000.pdf",
            warrantyPath = "Samsung_Warranty.pdf",
            rating = 4.3f,
            isFavorite = false
        ),

        // ==================== LG ====================
        Product(
            name = "LG ArtCool Inverter 12000 BTU (Wifi)",
            brand = "LG",
            model = "S12EW",
            btu = 12000,
            price = 13999.99,
            description = "LG Premium ArtCool inverter with WiFi connectivity and smart features",
            imagePath = "LG_ArtCool_12000.png",
            brochurePath = "LG_ArtCool_12000.pdf",
            warrantyPath = "LG_Warranty.pdf",
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
            imagePath = "LG_Cassette_60000.png",
            brochurePath = "LGCassette.pdf",
            warrantyPath = "LG_Warranty.pdf",
            rating = 4.6f,
            isFavorite = false
        ),

        // ==================== Carrier ====================
        Product(
            name = "Carrier Hi Wall Air Conditioner",
            brand = "Carrier",
            model = "42QH12D",
            btu = 12000,
            price = 9999.99,
            description = "Carrier Hi Wall air conditioner with efficient cooling and reliable performance",
            imagePath = "Carrier_HiWall.png",
            brochurePath = "CarrierHiWall.pdf",
            warrantyPath = "Carrier_Warranty.pdf",
            rating = 4.2f,
            isFavorite = false
        ),

        // ==================== Blu Star ====================
        Product(
            name = "Blu Star Cassette Air Conditioner",
            brand = "Blu Star",
            model = "BSC-12C",
            btu = 12000,
            price = 10999.99,
            description = "Blu Star cassette air conditioner with high cooling capacity",
            imagePath = "BluStar_Cassette.png",
            brochurePath = "BluStar_Cassette.pdf",
            warrantyPath = "BluStar_Warranty.pdf",
            rating = 4.0f,
            isFavorite = false
        ),

        // ==================== Daikin ====================
        Product(
            name = "Daikin Emura Wall-Mounted Air Conditioner",
            brand = "Daikin",
            model = "FTXJ-EMURA",
            btu = 12000,
            price = 22999.99,
            description = "Daikin Emura premium wall-mounted air conditioner with advanced inverter technology",
            imagePath = "Daikin_Emura_wall-mounted_air_conditioner.png",
            brochurePath = "Daikin_Emura.pdf",
            warrantyPath = "Daikin_Warranty.pdf",
            rating = 4.8f,
            isFavorite = false
        ),

        // ==================== Hisense ====================
        Product(
            name = "Hisense Wall-Mounted Split Air Conditioner",
            brand = "Hisense",
            model = "AS-12UW4RXC1",
            btu = 12000,
            price = 9999.99,
            description = "Hisense wall-mounted split air conditioner with WiFi control and A Energy Class rating",
            imagePath = "Hisense_wall-mounted_split_air_conditioner.png",
            brochurePath = "Hisense_WallMount.pdf",
            warrantyPath = "Hisense_Warranty.pdf",
            rating = 4.5f,
            isFavorite = false
        ),

        // ==================== TCL ====================
        Product(
            name = "TCL Wall-Mounted Split Air Conditioner",
            brand = "TCL",
            model = "TAC-12CHSA",
            btu = 12000,
            price = 7999.99,
            description = "TCL wall-mounted split air conditioner with inverter technology and fast cooling",
            imagePath = "TCL_wall-mounted_split_air_conditioner.png",
            brochurePath = "TCL_Split_12000.pdf",
            warrantyPath = "TCL_Warranty.pdf",
            rating = 4.1f,
            isFavorite = false
        ),

        // ==================== IQ ====================
        Product(
            name = "IQ Blackmirror Inverter 12000 BTU",
            brand = "IQ",
            model = "IQ-12INV",
            btu = 12000,
            price = 8999.99,
            description = "IQ Blackmirror inverter split air conditioner with WiFi control and digital display",
            imagePath = "IQ_Blackmirror_12000_btu.png",
            brochurePath = "IQ_Inverter_12000.pdf",
            warrantyPath = "IQ_Warranty.pdf",
            rating = 4.2f,
            isFavorite = false
        ),

        // ==================== Comfee ====================
        Product(
            name = "Comfee Split Air Conditioner (Indoor Unit)",
            brand = "Comfee",
            model = "CF-12V",
            btu = 12000,
            price = 7499.99,
            description = "Comfee inverter split air conditioner with energy-efficient cooling",
            imagePath = "Comfee_split_air_conditioner_indoor_unit.png",
            brochurePath = "Comfee_Inverter_12000.pdf",
            warrantyPath = "Comfee_Warranty.pdf",
            rating = 4.0f,
            isFavorite = false
        ),

        // ==================== Commercial / Cassette ====================
        Product(
            name = "Ceiling Cassette Air Conditioner",
            brand = "Alliance",
            model = "CAS-CEIL-12",
            btu = 12000,
            price = 15999.99,
            description = "Ceiling cassette air conditioner with 4-way airflow for commercial spaces",
            imagePath = "ceiling_cassette_air_conditioner.png",
            brochurePath = "Cassette_Aircon.pdf",
            warrantyPath = "Alliance_Warranty.pdf",
            rating = 4.3f,
            isFavorite = false
        ),
        Product(
            name = "Alliance Light Commercial Inverter Cassette",
            brand = "Alliance",
            model = "AIC-COM-INV",
            btu = 36000,
            price = 32999.99,
            description = "Alliance light commercial inverter cassette — ideal for offices and retail spaces",
            imagePath = "Alliance_Light_Commercial_Inverter_Cassette.png",
            brochurePath = "Alliance_Commercial.pdf",
            warrantyPath = "Alliance_Warranty.pdf",
            rating = 4.6f,
            isFavorite = false
        ),
        Product(
            name = "Blu Star Outdoor Condenser Unit",
            brand = "Blu Star",
            model = "BSC-OUT-12",
            btu = 12000,
            price = 9499.99,
            description = "Blu Star outdoor condenser unit for split air conditioning systems",
            imagePath = "Blue_Star_outdoor_condenser_unit.png",
            brochurePath = "BluStar_Outdoor.pdf",
            warrantyPath = "BluStar_Warranty.pdf",
            rating = 4.1f,
            isFavorite = false
        ),
        Product(
            name = "Carrier Mid-Wall 24000 BTU",
            brand = "Carrier",
            model = "42QH24D",
            btu = 24000,
            price = 18499.99,
            description = "Carrier mid-wall split air conditioner with powerful 24000 BTU cooling",
            imagePath = "24000_btu_Carrier_Midwall.png",
            brochurePath = "Carrier_Midwall_24000.pdf",
            warrantyPath = "Carrier_Warranty.pdf",
            rating = 4.4f,
            isFavorite = false
        )
    )
}