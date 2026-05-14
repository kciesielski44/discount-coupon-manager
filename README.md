# Discount Coupon Manager

REST-owy serwis kuponów rabatowych.



## Zakres

Serwis udostępnia dwie operacje biznesowe:

- Tworzenie kuponu - unikalny kod kuponu, data utworzenia, max usages, current usages, kraj przeznaczenia kuponu (wg standardu ISO 3166-1 alpha-2).
- Rejestracja użycia kuponu - z walidacją: istnienie kodu, dostępne użycia, dozwolony kraj (na przykład na podstawie IP), unikalność per użytkownik (opcjonalnie).



## Założenia

- Brak uwierzytelniania,
- Kod kuponu porównywany case-insensitive - w domenie i w bazie trzymany w jednej, znormalizowanej (lower case) formie. Unikalność wymusza zwykły UNIQUE na surowej kolumnie - bez funkcyjnego indeksu, bo wszystkie wartości wchodzące do bazy są już znormalizowane przez value object 'CouponCode' w warstwie domenowej.
- Kraj kuponu w formacie ISO 3166-1 alpha-2 (PL, DE, GB, itd.).
- wyznaczanie IP/kraju klienta - TODO do doprecyzowania niżej
- Limit użyć obowiązuje globalnie - „kto pierwszy ten lepszy", odporne na concurrent requests.
- Kupony bez daty wygaśnięcia (brak informacji w wymaganiach).
- Identyfikator użytkownika w opcjonalnym scenariuszu — dowolny string, bez walidacji formatu.



## Wyznaczanie IP/kraju klienta

1) Źródło IP
Adres klienta brany z nagłówka "X-Forwarded-For", ale tylko jeśli żądanie przyszło od zaufanego proxy (lista konfigurowana przez "server.tomcat.remoteip.internal-proxies"). W innym wypadku XFF jest ignorowany a w jego miejsce wchodzi "remoteAddr". Bez tej dyscypliny nagłówek jest spoofowalny przez klienta — wystarczy "-H 'X-Forwarded-For: 8.8.8.8'", żeby ominąć restrykcję krajem.

2) IP prywatne/loopback/localhost - (127.0.0.0/8, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
Nie wołamy do geo-IP — i tak nie odpowie sensownie. Każdy taki IP zwraca kod błędu CLIENT_COUNTRY_UNRESOLVED (http 422), osobny od COUNTRY_NOT_ALLOWED (http 403) - bo to inna przyczyna i ułatwia diagnostykę.

3) Klient na VPN - jest traktowany jak każdy inny: kraj exit node'a z vpn = kraj klienta. Z perspektywy serwisu nie ma innej informacji do rozróżnienia. Jest to bardziej ograniczenie geo-IP niż luka.



## Stack — alternatywy i decyzje

Java 21,
Maven,
PostgreSQL + Flyway + JPA,
Testcontainers (Postgres),
Java Records (java 21 sprawia, że lombok jest tu zbędny),
Ręczne mappery (odrzucam MapStruct, ponieważ przy jednym agregacie konfiguracja jest dłuższa niż sam mapper)
Caffeine (konfiguracja przez Spring Cache, za mało tego na Redisa)
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
phase-7-per-user - Opcjonalne: jeden użytkownik, jedno użycie (FAZA POMINIĘTA Z UWAGI KILKA KOMPLIKACJI PODCZAS IMPLEMENTACJI I FINALNIE POPRZEZ BRAK CZASU)
phase-8-production-ready - Actuator, metryki Prometheus, Dockerfile


## Dlaczego tak a nie inaczej

- Atomic UPDATE w bazie - row lock w Postgresie jest raczej jedynym sensownym poziomem prawdy o stanie kuponu.
- Caffeine, nie Redis - IP/kraj prawie się nie zmienia, cache per-instancja wystarcza. Abstrakcja Spring Cache pozwala przesiąść się na Redis jedną zmianą CacheManager, jak realnie zacznie być potrzebny (np. > 5 instancji).
- ip-api.com (a nie ipapi.co / ipinfo.io) - darmowy plan bez klucza, do 45 req/min z IP, JSON. Najprostsze do uruchomienia w demo.
- Brak Lomboka i MapStructa - records Javy 21 załatwiają value objecty i agregaty. Ręczny mapper to min 20 linii plus mniejsza powierzchnia zależności.
- Testcontainers, nigdy H2 - żeby testy integracyjne nie kłamały na temat tego, jak zachowa się prawdziwy Postgres.


## Zagadnienia do ewentualnego rozwoju aplikacji (dodatkowe pomysły)

1) Publikacja zdarzenia "CouponUsed" do Kafki dla innych konsumentów w aplikacji: analityka, raporty, marketing (emaile, smsy) i inne w zależności od zapotrzebowania.
2) Idempotencja na POST /usages
   Klient wysyła żądanie, a odpowiedź gdzieś ginie po drodze (brak Wifi itd), więc wysyła drugi reqeust z tym samym body. Wtedy nie chcemy podwójnego inkrementu zużycia. 
   Może równać się to z reklamacją).
   Propozycja: nowa encja do śledzenia kluczy z Idempotency-Key przekazywnaych w nagłówku HTTP. Dodatkowo filtr lub interceptor przechwytujący endpoint: przychodzący sprawdza 
   w tabeli, dla istniejącego zwraca responseBody zapisany w bazie, a dla nowych przetwarza żądanie i zapisuje response w bazie.
3) Wygasanie aktywnych kuponów (dodanie pól "expiresAt" oraz "status" jako nowe informacje o kuponie; walidacja daty w locie przy próbie użycia kuponu, scheduler jako 
   sprzątanie statusów pod raporty, wyświetlanie na GUI lub dedykowanych eventów domenowych).



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

zwracający 1 = sukces lub 0 = wyczerpany. Po sukcesie adapter robi osobny  selecyt findByCode, który dzięki clearAutomatically=true trafia już do bazy i widzi świeży stan.
Dodatkowy SELECT po sukcesie (zamiast jednego query mamy dwa). W praktyce około 1ms na żądanie. Koszt niewielki.
Czy ochrona przed wyścigiem jest tutaj zaburzona/niepilnowana?  Wg mnie nie. Sam atomic UPDATE z warunkiem WHERE current_usages < max_usages 
jest identyczny w obu wersjach. Postgres serializuje konkurentów po row locku (kto pierwszy ten lepszy).
Dowód:
- CouponUsageIntegrationTest.registersUsageOnExistingCoupon - zwraca poprawne currentUsages=1, 2, itd.
- CouponConcurrencyIntegrationTest.exactlyMaxUsagesSucceedUnderConcurrentLoad - ma 10 sukcesów, 90 odmów, currentUsages=10 w bazie


3) spring-boot-starter-aop nie istnieje w Boot 4

Starter AOP wypadł z BOM. Wymieniłem go na bezpośrednie spring-aop + aspectjweaver (BOM nimi zarządza i obie biblioteki są na właściwej wersji). 
Bez nich Resilience4j (@CircuitBreaker, @Retry) i Spring Cache (@Cacheable) nie miałyby aspektów do podpięcia.



## Uruchomienie i test end to end

Wymagania
- Docker
- Wolne porty na hoście: 8080 (aplikacja), 9090 (Prometheus), 5432 (Postgres)

## Start całego stacka
docker compose up --build


Po starcie dostępne:

- API aplikacji - http://localhost:8080
- Prometheus UI - http://localhost:9090
- Postgres - localhost:5432 (db: coupons, user: coupons, hasło: coupons)



## Zatrzymanie:
1) zatrzymanie kontenerów
docker compose down
2) wyczyszczenie volume bazy
docker compose down -v



## Smoke test infrastruktury

curl http://localhost:8080/actuator/health
Oczekiwane: status:UP, groups:[liveness, readiness]


curl -s "http://localhost:9090/api/v1/targets?state=active"
W JSON-ie powinny być dwa active targety (coupons-app, prometheus)



## Scenariusze biznesowe end to end

Pełny przepływ pokrywa wszystkie 5 ścieżek logiki użycia: success, exhausted, countryBlocked, notFound, countryUnresolved.

1) Utworzenie kuponów - jeden na US, jeden na PL:

curl -i -X POST -H 'Content-Type: application/json' -d '{"code":"DEMO_US","maxUsages":3,"country":"US"}' http://localhost:8080/coupons
curl -i -X POST -H 'Content-Type: application/json' -d '{"code":"DEMO_PL","maxUsages":1,"country":"PL"}' http://localhost:8080/coupons

Oczekiwane dla każdego: http 201 Created, nagłówek location="/coupons/demo_us" (lub demo_pl), body z currentUsages=0



2) Trzy udane użycia kuponu US:

curl -i -X POST -H 'X-Forwarded-For: 8.8.8.8' http://localhost:8080/coupons/DEMO_US/usages

Powtórzenie trzykrotnie inkrementuje licznik currentUsages. 
Oczekiwane: http 200 OK, currentUsages rośnie 1, 2 i 3.



3) Wyczerpany kupon - czwarta próba:

curl -i -X POST -H 'X-Forwarded-For: 8.8.8.8' http://localhost:8080/coupons/DEMO_US/usages

Oczekiwane: http 409 Conflict, body code=COUPON_EXHAUSTED, message=...



4) Blokada krajem - kupon PL, klient z 8.8.8.8 (US):

curl -i -X POST -H 'X-Forwarded-For: 8.8.8.8' http://localhost:8080/coupons/DEMO_PL/usages

Oczekiwane: http 403 Forbidden, body code=COUNTRY_NOT_ALLOWED, message=...



5) Nieistniejący kupon:

curl -i -X POST -H 'X-Forwarded-For: 8.8.8.8' http://localhost:8080/coupons/NIE_MA_TAKIEGO/usages

Oczekiwane: http 404 Not Found, body code=COUPON_NOT_FOUND, message=...



6) Niemożliwy do ustalenia kraj - request bez X-Forwarded-For:

curl -i -X POST http://localhost:8080/coupons/DEMO_PL/usages

Bez nagłówka XFF aplikacja widzi adres proxy Dockerowego (np. 172.18.0.1). 
Oczekiwane: http 422 Unprocessable Entity, body code=CLIENT_COUNTRY_UNRESOLVED, message=...



## Weryfikacja metryk z /actuator/prometheus

curl -s http://localhost:8080/actuator/prometheus | grep -E '^(coupon_created_total|coupon_usage_attempts_total|geoip_lookup_seconds_count|cache_gets_total)' | sort



## Prometheus

http://localhost:9090 (w przeglądarce)
Powinny tam być dwa targety: coupons-app i prometheus, oba up

Przydatne zapytania PromQL:

1) coupon_usage_attempts_total 
Surowy licznik wszystkich prób z podziałem na result

2) sum by (result) (coupon_usage_attempts_total)
Rozkład wyników użycia

3) rate(geoip_lookup_seconds_count[5m])
RPS wywołań do ip-api

4) cache_gets_total{cache="geoip-lookups"}
Hit/miss cache geoIP



## Uruchomienie testów (poza Dockerem)

mvn -B verify

Spodziewana liczba testów: 62






