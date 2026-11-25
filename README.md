# SmartGrid — практична робота №3

Варіант 8: система розподілу електроенергії 
## Як працює застосунок

1. **Запуск**: стартовий екран читає токен з DataStore. Токен дійсний — одразу меню, інакше — вхід.
2. **Вхід** через API `POST api/auth/login`. Тестові облікові записи:
   - оператор — `operator@grid.ua` / `grid2026`
   - адміністратор — `admin@grid.ua` / `admin2026`
   Токен дійсний 10 хвилин; після завершення терміну сервер повертає 401 і застосунок відкриває вхід.
3. **Головне меню**: ім'я та роль з токена. Пункт «Адміністрування» бачить лише адміністратор.
   - **Моніторинг мережі** — стан мережі з сервера + прогноз навантаження на 12 год за погодою Open-Meteo;
   - **Енергооб'єкти** — 12 об'єктів мережі Києва з API, кеш у Room (5 хв), фільтр, сортування, зведення;
   - **Режими мережі** — режим зберігає сервер; змінювати може лише адміністратор (оператору — 403);
   - **Розрахунок лінії**, **Журнал вимірювань**, **Нове вимірювання** — з практичних №1–2;
   - **Адміністрування** — користувачі (захищений ресурс), перевірка помилок 401/404/500/тайм-аут, журнал помилок.

## Сервер

Сервер диспетчерського центру імітується перехоплювачем `MockServerInterceptor` (mocked API):
він останній у ланцюжку OkHttp і повертає JSON-відповіді з HTTP-кодами, тому Retrofit і Gson працюють
як із реальним сервером. Приблизно кожен 6-й запит даних отримує 503 — так видно повторні спроби.
Погода береться зі справжнього зовнішнього сервісу `api.open-meteo.com` (потрібен інтернет).

Перевірка відсутності мережі: увімкнути режим польоту на емуляторі й натиснути «Оновити».

## Структура

```
app/src/main/java/ua/kpi/smartgrid/
├── MainActivity.kt, SmartGridApplication.kt   створення бази, джерел даних і репозиторіїв
├── navigation/        Screen, AppNavHost (стартовий екран, реакція на завершення сесії)
├── data/
│   ├── local/         Room (Entities, Daos, AppDatabase), LocalDataSource, TokenStore (DataStore)
│   ├── remote/        GridApi + WeatherApi (Retrofit), Dto (JSON), ApiClient, MockServerInterceptor,
│   │                  RemoteDataSource
│   ├── auth/          Session + JwtUtils (розбір токена), AuthInterceptor (Bearer-токен, 401)
│   ├── GridRepository.kt   об'єкти (кеш + сервер), стан мережі, прогноз, вимірювання
│   └── AuthRepository.kt   вхід, реєстрація, збереження і відновлення сесії
├── network/           NetworkChecker, ApiCaller (повторні спроби), ErrorHandler, ErrorLogger
├── domain/            Calculator, Validator, LoadForecast, GridLabels
├── viewmodel/         Auth, Monitoring, Objects, Measurement, Admin
└── ui/                screens/ і components/
```

Запуск: Android Studio → File → Open → папка проєкту → Gradle Sync → Run.
