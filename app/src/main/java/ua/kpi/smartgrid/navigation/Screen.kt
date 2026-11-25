package ua.kpi.smartgrid.navigation

/**
 * Маршрути застосунку (sealed class — усі екрани в одному місці).
 */
sealed class Screen(val route: String) {
    object Start : Screen("start")        // перевірка збереженого токена
    object Login : Screen("login")
    object Register : Screen("register")
    object Menu : Screen("menu")          // ім'я та роль беруться з токена
    object Monitoring : Screen("monitoring")
    object Objects : Screen("objects")
    object Modes : Screen("modes")
    object Calculator : Screen("calculator")
    object Measurements : Screen("measurements")
    object Admin : Screen("admin")        // лише для адміністратора

    // Форма вимірювання: id = 0 — новий запис, інакше — редагування
    object MeasurementForm : Screen("measurement_form/{id}") {
        fun create(id: Int) = "measurement_form/$id"
    }
}
