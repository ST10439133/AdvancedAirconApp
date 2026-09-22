// app/src/main/java/com/insy7315/advancedaircornapp/data/PartCatalog.kt
package com.insy7315.advancedaircornapp.data
data class CatalogPart(
    val name: String,
    val price: Double,
    val category: String = "General"
)

object PartCatalog {
    val availableParts: List<CatalogPart> = listOf(
        CatalogPart("R410A Refrigerant (1kg)", 180.00, "Refrigerants"),
        CatalogPart("R134A Refrigerant (1kg)", 220.00, "Refrigerants"),
        CatalogPart("R32 Refrigerant (1kg)", 200.00, "Refrigerants"),
        CatalogPart("Copper Tubing (15m)", 350.00, "Tubing"),
        CatalogPart("Copper Tubing (50ft)", 120.00, "Tubing"),
        CatalogPart("Contactor (30-Amp)", 85.00, "Electrical"),
        CatalogPart("Run Capacitor (45 uF)", 65.00, "Electrical"),
        CatalogPart("Thermostat (Digital)", 150.00, "Electrical"),
        CatalogPart("Condenser Fan Motor", 250.00, "Motors"),
        CatalogPart("Blower Motor (1/2 HP)", 280.00, "Motors"),
        CatalogPart("Air Filter (16x25x1)", 30.00, "Filters"),
        CatalogPart("Compressor (1.5 Ton)", 450.00, "Compressor"),
        CatalogPart("Compressor (2 Ton)", 520.00, "Compressor"),
        CatalogPart("AC Maintenance Kit", 340.00, "Maintenance"),
        CatalogPart("Coil Cleaner (1L)", 35.00, "Maintenance")
    )
}