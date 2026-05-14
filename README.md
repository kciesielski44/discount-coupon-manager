# Discount Coupon Manager

REST-owy serwis kuponów rabatowych.

## Zakres

Serwis udostępnia dwie operacje biznesowe:

- Tworzenie kuponu — unikalny kod kuponu, data utworzenia, max usages, current usages, kraj przeznaczenia kuponu (wg standardu ISO 3166-1 alpha-2).
- Rejestracja użycia kuponu — z walidacją: istnienie kodu, dostępne użycia, dozwolony kraj (na przykład na podstawie IP), unikalność per użytkownik (opcjonalnie).

## Założenia

- Brak uwierzytelniania,
- Kod kuponu porównywany case-insensitive - w domenie i w bazie trzymany w jednej, znormalizowanej (lower case) formie. Unikalność wymusza zwykły UNIQUE na surowej kolumnie - bez funkcyjnego indeksu, bo wszystkie wartości wchodzące do bazy są już znormalizowane przez value object 'CouponCode' w warstwie domenowej.
- Kraj kuponu w formacie ISO 3166-1 alpha-2 (PL, DE, GB, itd.).
- wyznaczanie IP/kraju klienta - TODO do doprecyzowania niżej
- Limit użyć obowiązuje globalnie - „kto pierwszy ten lepszy", odporne na concurrent requests.
- Kupony bez daty wygaśnięcia (brak informacji w wymaganiach).
- Identyfikator użytkownika w opcjonalnym scenariuszu — dowolny string, bez walidacji formatu.

### Wyznaczanie IP/kraju klienta

1) Źródło IP
Adres klienta brany z nagłówka "X-Forwarded-For", ale tylko jeśli żądanie przyszło od zaufanego proxy (lista konfigurowana przez "server.tomcat.remoteip.internal-proxies"). W innym wypadku XFF jest ignorowany a w jego miejsce wchodzi "remoteAddr". Bez tej dyscypliny nagłówek jest spoofowalny przez klienta — wystarczy "-H 'X-Forwarded-For: 8.8.8.8'", żeby ominąć restrykcję krajem.

2) IP prywatne/loopback/localhost - (127.0.0.0/8, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
Nie wołamy do geo-IP — i tak nie odpowie sensownie. Każdy taki IP zwraca kod błędu CLIENT_COUNTRY_UNRESOLVED (http 422), osobny od COUNTRY_NOT_ALLOWED (http 403) - bo to inna przyczyna i ułatwia diagnostykę.

3) Klient na VPN - jest traktowany jak każdy inny: kraj exit node'a z vpn = kraj klienta. Z perspektywy serwisu nie ma innej informacji do rozróżnienia. Jest to bardziej ograniczenie geo-IP niż luka.

### Przykładowe wywołania (curl)

Tworzenie kuponu z limitem 100 użyć dla Polski:


curl -X POST http://localhost:8080/coupons -H 'Content-Type: application/json' -d '{"code":"WIOSNA2026","maxUsages":100,"country":"PL"}'

Odpowiedź: 201 Created, body z "currentUsages": 0



Użycie kuponu bez nagłówka XFF 

curl -X POST http://localhost:8080/coupons/WIOSNA2026/usages



Użycie kuponu z nagłówkiem XFF

curl -X POST http://localhost:8080/coupons/WIOSNA2026/usages -H 'X-Forwarded-For: 8.8.8.8'

Odpowiedź: 200 OK, body z "currentUsages": 1.



Próba użycia z niedozwolonego kraju (kupon ma country: PL, IP wskazuje na DE):

curl -X POST http://localhost:8080/coupons/WIOSNA2026/usages -H 'X-Forwarded-For: 81.169.145.74'

Odpowiedź: 403 Forbidden, body {"code":"COUNTRY_NOT_ALLOWED", ...}.



Próba użycia kuponu, który osiągnął limit:

curl -X POST http://localhost:8080/coupons/WIOSNA2026/usages -H 'X-Forwarded-For: 8.8.8.8'

Odpowiedź: 409 Conflict, body {"code":"COUPON_EXHAUSTED", ...}.

## Stack — alternatywy i decyzje

Java 21,
Maven,
PostgreSQL + Flyway + JPA,
Testcontainers (Postgres),
Java Records (java 21 sprawia, że lombok jest tu zbędny),
Ręczne mappery (odrzucam MapStruct, ponieważ przy jednym agregacie konfiguracja jest dłuższa niż sam mapper)
Caffeine (konfiguracja przez Spring Cache)
Resilience4j (timeout, retry, circuit breaker)
JUnit 5 + AssertJ + Mockito + WireMock 
JSON ("logstash-logback-encoder") + MDC z request-id
Dockerfile (multi-stage) + docker-compose


## Roadmapaa

Implementacja podzielona na fazy:

phase-0-setup - wstępny plan i roadmapa w README + szkielet Maven/Spring
phase-1-domain - Agregat "Coupon", encja JPA, repozytorium z case-insensitive
phase-2-creation - POST /coupons
phase-3-usage - POST /coupons/{code}/usages — happy path, jeszcze bez kraju
phase-4-errors - Strukturalne błędy: "COUPON_NOT_FOUND", "COUPON_EXHAUSTED"
phase-5-concurrency - Atomic UPDATE counter — odporność na race condition
phase-6-geoip - Geo-IP + cache + Resilience4j + ograniczenie krajem
phase-7-per-user - Opcjonalne: jeden użytkownik, jedno użycie
phase-8-production-ready - Actuator, metryki Prometheus, JSON logging, Dockerfile, GitHub Actions


## Dlaczego tak a nie inaczej

- Atomic UPDATE w bazie - row lock w Postgresie jest raczej jedynym sensownym poziomem prawdy o stanie kuponu.
- Caffeine, nie Redis - IP/kraj prawie się nie zmienia, cache per-instancja wystarcza. Abstrakcja Spring Cache pozwala przesiąść się na Redis jedną zmianą CacheManager, jak realnie zacznie być potrzebny (np. > 5 instancji).
- ip-api.com (a nie ipapi.co / ipinfo.io) - darmowy plan bez klucza, do 45 req/min z IP, JSON. Najprostsze do uruchomienia w demo.
- Brak Lomboka i MapStructa - records Javy 21 załatwiają value objecty i agregaty. Ręczny mapper to min 20 linii plus mniejsza powierzchnia zależności.
- Testcontainers, nigdy H2 - żeby testy integracyjne nie kłamały na temat tego, jak zachowa się prawdziwy Postgres.



## Co poszło inaczej niż w planie

1) Flyway nie startował pod Spring Boot 4

W pomie miałem zwykłe flyway-core. Aplikacja wstaje, ale dostaję missing table coupons, mimo że migracja V1__coupons.sql jest na classpathie.
Błąd wynikał z użycia nowego Spring Boot 4. W nowym Spring Boocie podzielono monolityczny spring-boot-autoconfigure na osobne moduły dla każdej technologii. 
FlywayAutoConfiguration siedzi teraz w osobnym artefakcie spring-boot-flyway, którego flyway-core nie wciąga. Bez tej autokonfiguracji Flyway 
nigdy by się nie odpalił.
Wymieniłem flyway-core na spring-boot-starter-flyway, który sam zaciąga również flyway-core oraz moduł z autokonfiguracją.
Aplikacja startuje. W logach widać "Flyway: Migrating schema "public" to version "1 - coupons"."



2) RETURNING * vs cache Hibernata

Problem okazał się niewidoczny gołym okiem.

UPDATE coupons SET current_usages = current_usages + 1
 WHERE code = :code AND current_usages < max_usages
RETURNING *

Jedno query do bazy, zwraca świeży stan wiersza po inkrementacji. Klient dostaje nową wartość licznika bez dodatkowego SELECT-a. 
Test współbieżności (100 wątków × max 10 użyć) potwierdzał, że limit się trzyma (żadnych przekroczeń).

W fazie 6 dorzuciłem do UseCouponService sprawdzenie kraju przed inkrementacją użycia kuponu. 

Schemat:
- findByCode(code) - ładuje encję do Persistence Context Hibernate'a
- sprawdź country
- UPDATE ... RETURNING *

I wtedy test CouponUsageIntegrationTest zaczął zwracać currentUsages=0 zamiast 1 po użyciu. Mimo że w bazie wartość była prawidłowa.
Pierwszym podejrzanym był Persistent Contexzt Hibernata.

Rozdzieliłem operację na dwa kroki:

@Modifying(clearAutomatically=true, flushAutomatically=true)
UPDATE coupons SET current_usages = current_usages + 1
WHERE code = :code AND current_usages < max_usages

zwracający 1 = sukces lub 0 = wyczerpany. Po sukcesie adapter robi osobny findByCode, który dzięki clearAutomatically=true trafia już do bazy i widzi świeży stan.
Dodatkowy SELECT po sukcesie (zamiast jednego query mamy dwa). W praktyce około 1ms na żądanie. Koszt niewielki.
Czy ochrona przed wyścigiem jest tutaj zaburzona/niepilnowana?  Wg mnie nie. Sam atomic UPDATE z warunkiem WHERE current_usages < max_usages 
jest identyczny w obu wersjach. Postgres serializuje konkurentów po row locku (kto pierwszy ten lepszy).
Dowód:
- CouponUsageIntegrationTest.registersUsageOnExistingCoupon - zwraca poprawne currentUsages=1, 2, itd.
- CouponConcurrencyIntegrationTest.exactlyMaxUsagesSucceedUnderConcurrentLoad - ma 10 sukcesów, 90 odmów, currentUsages=10 w bazie



3) spring-boot-starter-aop nie istnieje w Boot 4

Starter AOP wypadł z BOM. Wymieniłem go na bezpośrednie spring-aop + aspectjweaver (BOM nimi zarządza i obie biblioteki są na właściwej wersji). 
Bez nich Resilience4j (@CircuitBreaker, @Retry) i Spring Cache (@Cacheable) nie miałyby aspektów do podpięcia.








