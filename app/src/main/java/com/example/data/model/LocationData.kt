package com.example.data.model

object LocationData {
    val provinces = listOf(
        "La Habana",
        "Artemisa",
        "Mayabeque",
        "Pinar del Río",
        "Matanzas",
        "Villa Clara",
        "Cienfuegos",
        "Sancti Spíritus",
        "Ciego de Ávila",
        "Camagüey",
        "Las Tunas",
        "Holguín",
        "Granma",
        "Santiago de Cuba",
        "Guantánamo",
        "Isla de la Juventud"
    )

    val municipalities = mapOf(
        "La Habana" to listOf(
            "Arroyo Naranjo", "Boyeros", "Centro Habana", "Cerro", "Cotorro",
            "Diez de Octubre", "Guanabacoa", "Habana del Este", "Habana Vieja",
            "La Lisa", "Marianao", "Playa", "Plaza de la Revolución", "Regla",
            "San Miguel del Padrón"
        ),
        "Artemisa" to listOf(
            "Alquízar", "Artemisa", "Bahía Honda", "Bauta", "Caimito",
            "Candelaria", "Guanajay", "Güira de Melena", "Mariel",
            "San Antonio de los Baños", "San Cristóbal"
        ),
        "Mayabeque" to listOf(
            "Batabanó", "Bejucal", "Güines", "Jaruco", "Madruga",
            "Melena del Sur", "Nueva Paz", "Quivicán", "San José de las Lajas",
            "San Nicolás", "Santa Cruz del Norte"
        ),
        "Pinar del Río" to listOf(
            "Consolación del Sur", "Guane", "La Palma", "Los Palacios",
            "Mantua", "Minas de Matahambre", "Pinar del Río", "San Juan y Martínez",
            "San Luis", "Sandino", "Viñales"
        ),
        "Matanzas" to listOf(
            "Calimete", "Cárdenas", "Ciénaga de Zapata", "Colón", "Jagüey Grande",
            "Jovellanos", "Limonar", "Los Arabos", "Martí", "Matanzas",
            "Pedro Betancourt", "Perico", "Unión de Reyes"
        ),
        "Villa Clara" to listOf(
            "Caibarién", "Camajuaní", "Cifuentes", "Corralillo", "Encrucijada",
            "Manicaragua", "Placetas", "Quemado de Güines", "Ranchuelo",
            "Remedios", "Sagua la Grande", "Santa Clara", "Santo Domingo"
        ),
        "Cienfuegos" to listOf(
            "Abreus", "Aguada de Pasajeros", "Cienfuegos", "Cruces",
            "Cumanayagua", "Lajas", "Palmira", "Rodas"
        ),
        "Sancti Spíritus" to listOf(
            "Cabaiguán", "Fomento", "Jatibonico", "La Sierpe", "Sancti Spíritus",
            "Taguasco", "Trinidad", "Yaguajay"
        ),
        "Ciego de Ávila" to listOf(
            "Baraguá", "Bolivia", "Chambas", "Ciego de Ávila", "Ciro Redondo",
            "Florencia", "Majagua", "Morón", "Primero de Enero", "Venezuela"
        ),
        "Camagüey" to listOf(
            "Camagüey", "Carlos Manuel de Céspedes", "Esmeralda", "Florida",
            "Guáimaro", "Jimaguayú", "Minas", "Najasa", "Nuevitas",
            "Santa Cruz del Sur", "Sibanicú", "Sierra de Cubitas", "Vertientes"
        ),
        "Las Tunas" to listOf(
            "Amancio", "Colombia", "Jesús Menéndez", "Jobabo", "Las Tunas",
            "Majibacoa", "Manatí", "Puerto Padre"
        ),
        "Holguín" to listOf(
            "Antilla", "Báguanos", "Banes", "Cacocum", "Calixto García",
            "Cueto", "Frank País", "Gibara", "Holguín", "Mayarí", "Moa",
            "Rafael Freyre", "Sagua de Tánamo", "Urbano Noris"
        ),
        "Granma" to listOf(
            "Bartolomé Masó", "Bayamo", "Buey Arriba", "Campechuela",
            "Cauto Cristo", "Guisa", "Jiguaní", "Manzanillo", "Media Luna",
            "Niquero", "Pilón", "Río Cauto", "Yara"
        ),
        "Santiago de Cuba" to listOf(
            "Contramaestre", "Guamá", "Mella", "Palma Soriano", "San Luis",
            "Santiago de Cuba", "Segundo Frente", "Tercer Frente", "Songo - La Maya"
        ),
        "Guantánamo" to listOf(
            "Baracoa", "Caimanera", "El Salvador", "Guantánamo", "Imías",
            "Manuel Tames", "Maisí", "Niceto Pérez", "San Antonio del Sur", "Yateras"
        ),
        "Isla de la Juventud" to listOf(
            "Isla de la Juventud"
        )
    )

    fun getMunicipalitiesForProvince(province: String): List<String> {
        return municipalities[province] ?: emptyList()
    }
}
