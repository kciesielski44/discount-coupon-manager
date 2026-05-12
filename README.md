# Discount Coupon Manager

REST-owy serwis kuponów rabatowych.

## Zakres

Serwis udostępnia dwie operacje biznesowe:

- Tworzenie kuponu — unikalny kod kuponu, data utworzenia, max usages, current usages, kraj przeznaczenia kuponu (wg standardu ISO 3166-1 alpha-2).
- Rejestracja użycia kuponu — z walidacją: istnienie kodu, dostępne użycia, dozwolony kraj (na przykład na podstawie IP), unikalność per użytkownik (opcjonalnie).

## Założenia

- Brak uwierzytelniania,
- Kod kuponu porównywany case-insensitive; w bazie trzymany w jednej, znormalizowanej formie. Unikalność wymuszamy indeksem na znormalizowanym kodzie (wstępnie, do potwierdzenia w trakcie implmentacji).
- Kraj kuponu w formacie ISO 3166-1 alpha-2 (PL, DE, GB, itd.).
- wyznaczanie IP/kraju klienta - TODO do doprecyzowania niżej
- Limit użyć obowiązuje globalnie — „kto pierwszy ten lepszy", odporne na concurrent requests.
- Kupony bez daty wygaśnięcia (brak informacji w wymaganiach).
- Identyfikator użytkownika w opcjonalnym scenariuszu — dowolny string, bez walidacji formatu.

### Wyznaczanie IP/kraju klienta

1) Źródło IP
Adres klienta brany z nagłówka "X-Forwarded-For", ale tylko jeśli żądanie przyszło od zaufanego proxy (lista konfigurowana przez "server.tomcat.remoteip.internal-proxies"). W innym wypadku XFF jest ignorowany a w jego miejsce wchodzi "remoteAddr". Bez tej dyscypliny nagłówek jest spoofowalny przez klienta — wystarczy "-H 'X-Forwarded-For: 8.8.8.8'", żeby ominąć restrykcję krajem.

2) IP prywatne/loopback/localhost - (127.0.0.0/8, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
Nie wołamy do geo-IP — i tak nie odpowie sensownie. Traktujemy je jako „kraj niemożliwy do ustalenia":
  - profil "prod": odrzucanie z kodem "CLIENT_COUNTRY_UNRESOLVED" (osobny od "COUNTRY_NOT_ALLOWED", bo to inna przyczyna i ułatwia diagnostykę)
  - profil "local": honorowanie XFF od dowolnego źródła (na potrzeby ręcznych testów) — (TODO dodać niżej przykład curla i instrukcję testowania ?? )
  
3) Klient na VPN -  jest traktowany jak każdy inny: kraj exit node'a z vpn = kraj klienta. Z perspektywy serwisu nie ma innej rozróżnialnej informacji — to świadoma akceptacja ograniczenia geo-IP, nie luka.

## Stack — alternatywy i decyzje

Java 21,
Maven,
PostgreSQL + Flyway + JPA,
Testcontainers (Postgres),
Java Records (java 21 sprawia, że lombok jest tu zbędny),
Ręczne mappery (odrzucam MapStruct, ponieważ przy jednym agregacie konfiguracja jest dłuższa niż sam mapper)
Caffeine (konfiguracja przez Spring Cache)
Resilience4j (timeout, retry)
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

cdn...
