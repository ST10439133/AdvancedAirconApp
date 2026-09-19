package com.prog7314.arcticflow.data

/**
 * Static lookup for South African provinces → cities → suburbs.
 *
 * This is intentionally a plain `object` with nested maps, so it works
 * without any network calls and any Compose screen can consume it directly.
 *
 * Data is a curated starter set — feel free to extend each list.
 */
object SouthAfricaLocations {

    /** All 9 provinces in the standard order. */
    val provinces: List<String> = listOf(
        "Eastern Cape",
        "Free State",
        "Gauteng",
        "KwaZulu-Natal",
        "Limpopo",
        "Mpumalanga",
        "Northern Cape",
        "North West",
        "Western Cape"
    )

    /** province to cities in that province */
    private val citiesByProvince: Map<String, List<String>> = mapOf(
        "Eastern Cape" to listOf(
            "Port Elizabeth (Gqeberha)", "East London", "Mthatha",
            "Queenstown (Komani)", "Grahamstown (Makhanda)", "Uitenhage"
        ),
        "Free State" to listOf(
            "Bloemfontein", "Welkom", "Bethlehem", "Sasolburg", "Kroonstad"
        ),
        "Gauteng" to listOf(
            "Johannesburg", "Pretoria", "Sandton", "Midrand", "Centurion",
            "Randburg", "Roodepoort", "Soweto", "Kempton Park",
            "Benoni", "Boksburg", "Germiston", "Springs", "Vereeniging",
            "Vanderbijlpark", "Krugersdorp", "Alberton"
        ),
        "KwaZulu-Natal" to listOf(
            "Durban", "Pietermaritzburg", "Richards Bay", "Newcastle",
            "Ballito", "Umhlanga", "Pinetown", "Port Shepstone"
        ),
        "Limpopo" to listOf(
            "Polokwane", "Tzaneen", "Thohoyandou", "Lephalale", "Mokopane"
        ),
        "Mpumalanga" to listOf(
            "Mbombela (Nelspruit)", "Emalahleni (Witbank)", "Secunda",
            "Middelburg", "Ermelo", "Standerton"
        ),
        "Northern Cape" to listOf(
            "Kimberley", "Upington", "Springbok", "Kuruman", "De Aar"
        ),
        "North West" to listOf(
            "Rustenburg", "Mahikeng (Mafikeng)", "Potchefstroom",
            "Klerksdorp", "Brits"
        ),
        "Western Cape" to listOf(
            "Cape Town", "Stellenbosch", "Paarl", "George", "Knysna",
            "Mossel Bay", "Hermanus", "Worcester", "Bellville", "Somerset West"
        )
    )

    /** "province|city" to suburbs in that city */
    private val suburbsByCity: Map<String, List<String>> = mapOf(
        // ============ GAUTENG ============
        "Gauteng|Johannesburg" to listOf(
            "Sandton", "Rosebank", "Fourways", "Randburg", "Midrand",
            "Bryanston", "Parktown", "Braamfontein", "Maboneng",
            "Kensington", "Observatory", "Melville", "Greenside",
            "Houghton", "Rosebank", "Rivonia", "Sunninghill",
            "Linden", "Northcliff", "Fairland"
        ),
        "Gauteng|Pretoria" to listOf(
            "Hatfield", "Brooklyn", "Menlyn", "Centurion", "Waterkloof",
            "Silver Lakes", "Pretoria East", "Pretoria West",
            "Sunnyside", "Arcadia", "Lynnwood"
        ),
        "Gauteng|Sandton" to listOf(
            "Bryanston", "Morningside", "Rivonia", "Sunninghill",
            "Hyde Park", "Rosebank", "Wendywood", "Killarney"
        ),
        "Gauteng|Midrand" to listOf(
            "Halfway House", "Vorna Valley", "Carlswald",
            "Blue Hills", "Kyalami", "Olifantsfontein"
        ),
        "Gauteng|Centurion" to listOf(
            "Centurion CBD", "Eldoraigne", "Wierdapark", "Rooihuiskraal",
            "Highveld", "Irene", "Lyttelton"
        ),
        "Gauteng|Randburg" to listOf(
            "Ferndale", "Bryanston", "Blairgowrie", "Linden",
            "Northcliff", "Fairland", "Bromhof"
        ),
        "Gauteng|Roodepoort" to listOf(
            "Florida", "Discovery", "Witpoortjie", "Wilropark",
            "Helderkruin", "Little Falls"
        ),
        "Gauteng|Soweto" to listOf(
            "Orlando", "Diepkloof", "Pimville", "Dobsonville", "Meadowlands"
        ),
        "Gauteng|Kempton Park" to listOf(
            "Birchleigh", "Edleen", "Norkem Park", "Kempton Park CBD"
        ),
        "Gauteng|Benoni" to listOf(
            "Benoni CBD", "Northmead", "Rynfield", "Farrarmere"
        ),
        "Gauteng|Boksburg" to listOf(
            "Boksburg CBD", "Boksburg North", "Sunward Park", "Parkrand"
        ),
        "Gauteng|Germiston" to listOf(
            "Germiston CBD", "Bedfordview", "Lambton", "Elsburg"
        ),
        "Gauteng|Springs" to listOf(
            "Springs CBD", "Selection Park", "Bakerton"
        ),
        "Gauteng|Vereeniging" to listOf(
            "Vereeniging CBD", "Three Rivers", "Peacehaven", "Roshnee"
        ),
        "Gauteng|Vanderbijlpark" to listOf(
            "Vanderbijlpark CBD", "SE1", "CE1", "Bophelong"
        ),
        "Gauteng|Krugersdorp" to listOf(
            "Krugersdorp CBD", "Noordheuwel", "Monument", "Rant-en-Dal"
        ),
        "Gauteng|Alberton" to listOf(
            "Alberton CBD", "Brackenhurst", "New Redruth", "Verwoerdpark"
        ),

        //  WESTERN CAPE
        "Western Cape|Cape Town" to listOf(
            "Sea Point", "Green Point", "Camps Bay", "Claremont",
            "Rondebosch", "Observatory", "Woodstock", "Salt River",
            "Constantia", "Tokai", "Muizenberg", "Fish Hoek",
            "Milnerton", "Century City", "Table View", "Bellville",
            "Durbanville", "Goodwood", "Pinelands", "Athlone",
            "Mitchells Plain", "Khayelitsha", "Gugulethu"
        ),
        "Western Cape|Stellenbosch" to listOf(
            "Stellenbosch Central", "Dalsig", "Paradyskloof",
            "Universiteitsoord", "Idasvallei"
        ),
        "Western Cape|Paarl" to listOf(
            "Paarl Central", "Noord-Paarl", "Suid-Paarl", "Courtrai"
        ),
        "Western Cape|George" to listOf(
            "George Central", "Blanco", "Heatherlands", "Pacaltsdorp"
        ),
        "Western Cape|Knysna" to listOf(
            "Knysna Central", "Belvidere", "Rheenendal", "Hornlee"
        ),
        "Western Cape|Mossel Bay" to listOf(
            "Mossel Bay Central", "Diaz Beach", "Hartenbos", "Kwanonqaba"
        ),
        "Western Cape|Hermanus" to listOf(
            "Hermanus Central", "Voelklip", "Onrusrivier", "Zwelihle"
        ),
        "Western Cape|Worcester" to listOf(
            "Worcester Central", "Roux Park", "De Doorns", "Zwelethemba"
        ),
        "Western Cape|Bellville" to listOf(
            "Bellville CBD", "Oakdale", "Kasselsvlei", "Boston"
        ),
        "Western Cape|Somerset West" to listOf(
            "Somerset West Central", "Helderberg", "Strand", "Gordons Bay"
        ),

        // ============ KWAZULU-NATAL ============
        "KwaZulu-Natal|Durban" to listOf(
            "Durban CBD", "Umhlanga", "Ballito", "Berea", "Morningside",
            "Glenwood", "Westville", "Pinetown", "Chatsworth",
            "Phoenix", "Umlazi", "Durban North", "Overport"
        ),
        "KwaZulu-Natal|Pietermaritzburg" to listOf(
            "PMB CBD", "Scottsville", "Pelham", "Hayfields", "Edendale"
        ),
        "KwaZulu-Natal|Richards Bay" to listOf(
            "Richards Bay CBD", "Meerensee", "Birdswood", "Arboretum"
        ),
        "KwaZulu-Natal|Newcastle" to listOf(
            "Newcastle CBD", "Aviary Hill", "Hutten Heights", "Madadeni"
        ),
        "KwaZulu-Natal|Ballito" to listOf(
            "Ballito Central", "Shakas Rock", "Salt Rock", "Umhlali"
        ),
        "KwaZulu-Natal|Umhlanga" to listOf(
            "Umhlanga Rocks", "La Lucia", "Mount Edgecombe", "Phoenix"
        ),
        "KwaZulu-Natal|Pinetown" to listOf(
            "Pinetown CBD", "New Germany", "Westville", "Kloof"
        ),
        "KwaZulu-Natal|Port Shepstone" to listOf(
            "Port Shepstone CBD", "Uvongo", "Margate", "Shelly Beach"
        ),

        // ============ EASTERN CAPE ============
        "Eastern Cape|Port Elizabeth (Gqeberha)" to listOf(
            "Summerstrand", "Humewood", "Walmer", "Newton Park",
            "Korsten", "Motherwell", "KwaZakhele", "Central"
        ),
        "Eastern Cape|East London" to listOf(
            "East London CBD", "Beacon Bay", "Nahoon", "Gonubie",
            "Cambridge", "Quigney"
        ),
        "Eastern Cape|Mthatha" to listOf(
            "Mthatha CBD", "Southernwood", "Norwood", "Ngangelizwe"
        ),
        "Eastern Cape|Queenstown (Komani)" to listOf(
            "Queenstown CBD", "Ezibeleni", "Mlungisi"
        ),
        "Eastern Cape|Grahamstown (Makhanda)" to listOf(
            "Grahamstown CBD", "Rhodes University", "Joza"
        ),
        "Eastern Cape|Uitenhage" to listOf(
            "Uitenhage CBD", "KwaNobuhle", "Rosedale"
        ),

        // ============ FREE STATE ============
        "Free State|Bloemfontein" to listOf(
            "Bloemfontein CBD", "Westdene", "Universitas", "Heidedal",
            "Mangaung", "Langenhoven Park"
        ),
        "Free State|Welkom" to listOf(
            "Welkom CBD", "Bedelia", "Riebeeckstad", "Thabong"
        ),
        "Free State|Bethlehem" to listOf(
            "Bethlehem CBD", "Bohlokong", "Jordania"
        ),
        "Free State|Sasolburg" to listOf(
            "Sasolburg CBD", "Vaalpark", "Zamdela"
        ),
        "Free State|Kroonstad" to listOf(
            "Kroonstad CBD", "Maokeng", "Constantia"
        ),

        // ============ LIMPOPO ============
        "Limpopo|Polokwane" to listOf(
            "Polokwane CBD", "Bendor", "Fauna Park", "Seshego", "Mankweng"
        ),
        "Limpopo|Tzaneen" to listOf(
            "Tzaneen CBD", "Aqua Park", "Nkowankowa"
        ),
        "Limpopo|Thohoyandou" to listOf(
            "Thohoyandou CBD", "Sibasa", "Shayandima"
        ),
        "Limpopo|Lephalale" to listOf(
            "Lephalale CBD", "Onverwacht", "Marapong"
        ),
        "Limpopo|Mokopane" to listOf(
            "Mokopane CBD", "Mahwelereng", "Mine Village"
        ),

        // ============ MPUMALANGA ============
        "Mpumalanga|Mbombela (Nelspruit)" to listOf(
            "Mbombela CBD", "Sonheuwel", "West Acres", "Riverside",
            "Kanyamazane"
        ),
        "Mpumalanga|Emalahleni (Witbank)" to listOf(
            "Emalahleni CBD", "Del Judor", "Reyno Ridge", "KwaGuqa"
        ),
        "Mpumalanga|Secunda" to listOf(
            "Secunda CBD", "Trichardt", "Evander", "Embalenhle"
        ),
        "Mpumalanga|Middelburg" to listOf(
            "Middelburg CBD", "Aerorand", "Mhluzi"
        ),
        "Mpumalanga|Ermelo" to listOf(
            "Ermelo CBD", "Wesselton", "Chrissiesmeer"
        ),
        "Mpumalanga|Standerton" to listOf(
            "Standerton CBD", "Sakhile", "Meyerville"
        ),

        // ============ NORTHERN CAPE ============
        "Northern Cape|Kimberley" to listOf(
            "Kimberley CBD", "Belgravia", "Galeshewe", "Roodepan"
        ),
        "Northern Cape|Upington" to listOf(
            "Upington CBD", "Progress", "Paballelo", "Louisvale"
        ),
        "Northern Cape|Springbok" to listOf(
            "Springbok CBD", "Bergsig", "Matjieskloof"
        ),
        "Northern Cape|Kuruman" to listOf(
            "Kuruman CBD", "Mothibistad", "Wrenchville"
        ),
        "Northern Cape|De Aar" to listOf(
            "De Aar CBD", "Nonzwakazi", "Bongolethu"
        ),

        // ============ NORTH WEST ============
        "North West|Rustenburg" to listOf(
            "Rustenburg CBD", "Waterfall East", "Tlhabane",
            "Boitekong", "Cashan"
        ),
        "North West|Mahikeng (Mafikeng)" to listOf(
            "Mahikeng CBD", "Montshioa", "Mmabatho", "Danville"
        ),
        "North West|Potchefstroom" to listOf(
            "Potchefstroom CBD", "Baillie Park", "Ikageng", "Grimbeekpark"
        ),
        "North West|Klerksdorp" to listOf(
            "Klerksdorp CBD", "Jouberton", "Alabama", "Wilkoppies"
        ),
        "North West|Brits" to listOf(
            "Brits CBD", "Oukasie", "Mooinooi"
        )
    )

    fun citiesFor(province: String): List<String> =
        citiesByProvince[province].orEmpty()

    fun suburbsFor(province: String, city: String): List<String> =
        suburbsByCity["$province|$city"].orEmpty()

    /**
     * Builds a single-line, comma-separated address suitable for
     * Google Maps geocoding and display.
     */
    fun composeFullAddress(
        street: String,
        suburb: String,
        city: String,
        province: String,
        postalCode: String
    ): String {
        return listOf(street, suburb, city, province, postalCode)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")
    }
}