package com.patson.init

import com.patson.model._
import com.patson.data.AirportSource
import com.patson.data.DestinationSource

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object AirportFeaturePatcher extends App {

  import AirportFeatureType._

  lazy val featureList = Map(

    INTERNATIONAL_HUB -> Map[String, Int](
      /**
       * international vacation destinations
       */
"DPS" -> 84, //Denpasar-Bali Island
"IST" -> 80, //Istanbul
"CDG" -> 80, //Paris
"AYT" -> 75, //Antalya
"CNX" -> 70, //Chiang Mai
"PQC" -> 70, //Phu Quoc Island
"HKT" -> 69, //Phuket
"HRG" -> 67, //Hurghada
"PUJ" -> 65, //Punta Cana
"DXB" -> 65, //Dubai
"CUN" -> 64, //Cancún
"HKG" -> 63, //Hong Kong
"BKK" -> 62, //Bangkok
"JED" -> 61, //Jeddah
"CUZ" -> 61, //Cusco
"LGK" -> 61, //Langkawi
"SSH" -> 60, //Sharm el-Sheikh
"USM" -> 60, //Na Thon (Ko Samui Island)
"LHR" -> 60, //London
"PNH" -> 59, //Phnom Penh
"RAK" -> 58, //Marrakech
"UTP" -> 58, //Rayong
"KBV" -> 57, //Krabi
"CXR" -> 56, //Nha Trang
"MLE" -> 55, //Malé Maldives
"JFK" -> 54, //New York
"NRT" -> 53, //Tokyo / Narita
"DAD" -> 50, //Da Nang
"BCN" -> 48, //Barcelona
"CMB" -> 48, //Colombo
"FCO" -> 46, //Rome
"DMK" -> 45, //Bangkok
"MBJ" -> 45, //Montego Bay
"HAV" -> 45, //Havana
"ASR" -> 44, //Kayseri
"RMF" -> 44, //Marsa Alam
"CZM" -> 44, //Cozumel
"KUL" -> 43, //Kuala Lumpur
"SIN" -> 42, //Singapore
"VCE" -> 42, //Venice
"MIA" -> 42, //Miami
"AMS" -> 41, //Amsterdam
"LAX" -> 41, //Los Angeles
"KIX" -> 40, //Osaka
"RUN" -> 40, //St Denis
"CAI" -> 40, //Cairo Egypt
"GRU" -> 39, //São Paulo
"GIG" -> 39, //Rio De Janeiro
"VIE" -> 39, //Vienna
"ATH" -> 39, //Athens
"CEB" -> 38, //Lapu-Lapu City
"GZP" -> 38, //Gazipaşa
"EWR" -> 38, //New York City USA
"LGW" -> 38, //London United Kingdom
"HND" -> 38, //Tokyo / Haneda
"AGP" -> 35, //Málaga
"CPT" -> 35, //Cape Town
"SCL" -> 35, //Santiago
"SPX" -> 35, //Cairo
"SJU" -> 34, //San Juan
"NCE" -> 34, //Nice
"BJV" -> 34, //Bodrum
"YYZ" -> 34, //Toronto Canada
"ICN" -> 34, //Seoul
"BKI" -> 33, //Kota Kinabalu
"LIS" -> 32, //Lisbon
"BER" -> 32, //Berlin
"SAI" -> 31, //Siem Reap
"AGA" -> 31, //Agadir
"SYD" -> 30, //Sydney Australia
"HNL" -> 30, //Honolulu
"AEP" -> 30, //Buenos Aires
"LOP" -> 30, //Mataram
"ARN" -> 30, //Stockholm
"POP" -> 29, //Puerto Plata Dominican Republic
"PEK" -> 29, //Beijing
"MXP" -> 29, //Milan
"DUB" -> 29, //Dublin Ireland
"MUC" -> 29, //Munich
"MEX" -> 29, //Mexico City
"MCO" -> 28, //Orlando
"MPH" -> 28, //Malay
"FUE" -> 28, //Fuerteventura Island
"PEN" -> 28, //Penang
"DJE" -> 28, //Djerba
"PRG" -> 28, //Prague
"PPT" -> 28, //Papeete
"AUA" -> 28, //Oranjestad
"BOM" -> 27, //Mumbai
"LPB" -> 27, //La Paz / El Alto
"VRA" -> 27, //Varadero
"TLV" -> 26, //Tel Aviv
"BUD" -> 26, //Budapest
"KEF" -> 26, //Reykjavík
"BAH" -> 25, //Manama
"POA" -> 25, //Porto Alegre
"GOI" -> 25, //Vasco da Gama
"YVR" -> 25, //Vancouver
"ZQN" -> 25, //Queenstown
"CMN" -> 25, //Casablanca
"RHO" -> 24, //Rodes Island
"MAD" -> 24, //Madrid
"LAP" -> 24, //La Paz
"PPS" -> 24, //Puerto Princesa City
"NOU" -> 24, //Nouméa
"PTP" -> 24, //Pointe-Ã -Pitre
"GUM" -> 23, //Hagåtña Guam International Airport
"KTM" -> 23, //Kathmandu
"CPH" -> 23, //Copenhagen
"SXM" -> 22, //Saint Martin
"MRU" -> 22, //Port Louis
"EZE" -> 22, //Buenos Aires
"BNE" -> 21, //Brisbane
"NAP" -> 21, //Nápoli
"IAD" -> 21, //Washington
"TRV" -> 21, //Thiruvananthapuram
"LXA" -> 21, //Lhasa
"SFO" -> 21, //San Francisco
"MQP" -> 21, //Mpumalanga
"SEZ" -> 21, //Mahe Island
"LIM" -> 21,
"PDL" -> 21, //Azores
"PMI" -> 20, //Palma De Mallorca
"LPA" -> 20, //Gran Canaria Island
"TFS" -> 20, //Tenerife Island
"VAR" -> 20, //Varna
"FLR" -> 20, //Firenze
"HER" -> 20, //Heraklion
"JMK" -> 20, //Mykonos Island
"FNC" -> 20, //Funchal
"LPQ" -> 20, //Luang Phabang
"OSL" -> 20, //Oslo
"CUR" -> 20, //Willemstad
"NAN" -> 20, //Nadi
"GAN" -> 20, //Maldives
"MEL" -> 19, //Melbourne
"LAS" -> 19, //Las Vegas
"IBZ" -> 19, //Ibiza
"JTR" -> 19, //Santorini Island
"SVO" -> 19, //Moscow
"BOJ" -> 19, //Burgas
"SJO" -> 19, //San Jose
"ZRH" -> 19, //Zurich
"EBB" -> 19, //Kampala
"CTG" -> 18, //Cartagena
"CNS" -> 18, //Cairns
"AKL" -> 18, //Auckland
"LVI" -> 18, //Livingstone
"PSA" -> 18, //Pisa
"KGS" -> 18, //Kos Island
"NLU" -> 18, //Mexico City
"DRW" -> 18, //Darwin
"MVD" -> 18, //Montevideo
"BGI" -> 18, //Bridgetown
"BVC" -> 18, //Rabil
"HEL" -> 18, //Helsinki
"MIR" -> 18, //Monastir
"PVG" -> 18, //Shanghai
"ORD" -> 18, //Chicago
"SGN" -> 18,
"OGG" -> 17, //Kahului
"STT" -> 17, //Charlotte Amalie
"CFU" -> 17, //Kerkyra Island
"LCA" -> 17, //Larnarca
"JRO" -> 17, //Arusha
"NBE" -> 17, //Enfidha
"RTB" -> 17, //Roatan Island
"ATL" -> 17,
"GPS" -> 17, //Baltra Galapagos
"PMO" -> 16, //Palermo
"MUB" -> 16, //Maun
"ZNZ" -> 16, //Zanzibar
"BGY" -> 16, //Milan
"FDF" -> 16, //Fort-de-France
"WAW" -> 16, //Warsaw
"BSL" -> 16, //Mulhouse French/Swiss Alps
"BOG" -> 16,
"TIA" -> 16, //Triana
"DOH" -> 16,
"AER" -> 15, //Sochi
"CAG" -> 15, //Cagliari
"COK" -> 15, //Kochi
"IKA" -> 15, //Tehran
"VKO" -> 15, //Moscow
"SKD" -> 15, //Samarkand
"ACE" -> 15, //Lanzarote Island
"ZTH" -> 15, //Zakynthos Island
"KIN" -> 15, //Kingston
"TIV" -> 15, //Tivat
"JNB" -> 15, //Johannesburg
"EDI" -> 15, //Edinburgh
"ADB" -> 15, //Izmir
"BOD" -> 15, //prehistoric caves France
"FLG" -> 15, //Flagstaff Grand Canyon
"SJD" -> 14, //San José del Cabo
"PVR" -> 14, //Puerto Vallarta
"PER" -> 14, //Perth
"NAS" -> 14, //Nassau
"KRK" -> 14, //Kraków
"AMM" -> 14, //Amman
"SID" -> 14, //Espargos
"YZF" -> 14, //Yellowknife
"DME" -> 14, //Moscow
"MNL" -> 14,
"MED" -> 13, //Medina
"LED" -> 13, //St. Petersburg
"KOS" -> 12, //Sihanukville
"CTA" -> 12, //Catania
"BTH" -> 12, //Batam Island
"DBV" -> 12, //Dubrovnik
"THR" -> 12, //Tehran
"TFN" -> 12, //Tenerife Island
"HUX" -> 12, //Huatulco
"PLZ" -> 12, //Addo Elephant National Park South Africa
"HDS" -> 12, //Kruger National Park South Africa
"PDP" -> 12, //Punta del Este
"MAO" -> 12, //Manaus
"TNM" -> 12, //AQ
"XIY" -> 12, //Xi'an
"TBS" -> 12, //Tbilisi
"RAI" -> 12, //Praia
"LBJ" -> 12, //Komodo National Park Indonesia
"VTE" -> 12, //Luang Prabang Laos
"SLC" -> 12, //Salt Lake City
"PTY" -> 12,
"JNU" -> 12, //Juneau
"VFA" -> 11, //Victoria Falls
"JAI" -> 11, //Jaipur
"LIR" -> 11, //Liberia Costa Rica
"ANC" -> 11, //Anchorage
"TER" -> 11, //Azores Lajes
"SAW" -> 10, //Istanbul
"FAO" -> 10, //Faro
"DEL" -> 10,
"HAN" -> 10, //Hanoi
"IGU" -> 10, //Foz Do IguaÃ§u
"CJC" -> 10, //Calama
"CHQ" -> 10, //Heraklion
"CIA" -> 10, //Ostia Antica Italy
"GYD" -> 10, //Baku
"YUL" -> 10, //Montreal
"MAH" -> 10, //Menorca Island
"TGZ" -> 10, //Tuxtla Gutiérrez
"SPU" -> 10, //Split
"PLS" -> 10, //Providenciales Turks and Caicos
"VER" -> 10, //Pico de Orizaba National Park Mexico
"BON" -> 10, //Kralendijk Bonaire
"CCC" -> 10, //Cayo Coco
"PFO" -> 10, //Paphos
"SRQ" -> 10, //Sarasota/Bradenton
"DEN" -> 10,
"AUH" -> 10,
"CGK" -> 10,
"TPE" -> 10,
"SEA" -> 10,
"REC" -> 9, //Recife
"IGR" -> 9, //Puerto Iguazu
"BRC" -> 9, //San Carlos de Bariloche
"NBO" -> 9, //Nairobi
"GND" -> 9,
"ECN" -> 9, //Nicosia
"LIF" -> 9, //Lifou
"GUA" -> 9, //Tikal Guatemala
"LJU" -> 9, //Triglav National Park Slovenia
"TGD" -> 9,
"SPC" -> 9,
"MSY" -> 8, //New Orleans
"TOS" -> 8, //Tromsø
"CCJ" -> 8, //Calicut
"BWN" -> 8, //Bandar Seri Begawan
"SMR" -> 8, //Santa Marta
"SSA" -> 8, //Salvador
"UVF" -> 8, //Vieux Fort
"FAT" -> 8, //Yosemite National Park USA
"GCN" -> 8, //Grand Canyon
"STX" -> 8, //Christiansted
"SZG" -> 8, //Salzburg Austrian Alps
"USH" -> 8, //Ushuahia
"UPN" -> 8, //Kgalagadi Transfrontier Park South Africa/Botswana
"GDT" -> 8, //Cockburn Town
"CYO" -> 8, //Cayo Largo del Sur Cuba
"SMA" -> 8, //Azores
"FLW" -> 8, //Azores Flores
"CGB" -> 8, //Cuiabá Ecotourism
"VDE" -> 8, //Canary Islands
"PTF" -> 8, //Mamanuca Islands
"SZG" -> 7, //Berchtesgaden National Park Germany
"BTS" -> 7, //Devin Castle Slovakia
"BEY" -> 7,
"GMZ" -> 7, //Canary Islands
"LXR" -> 6, //Luxor
"PNT" -> 6, //Torres del Paine National Park Chile
"BZE" -> 6, //Chiquibul National Park Belize
"SJZ" -> 6, //Azores São Jorge
"XIY" -> 5, //Terracotta Army China
"ASP" -> 5, //Alice Springs
"AYQ" -> 5, //Ayers Rock
"UNA" -> 5, //Transamérica Resort Comandatuba Island
"FAI" -> 5, //Fairbanks
"MFA" -> 5, //Mafia Island TZ
"SZG" -> 5, //Salzburg
"FCA" -> 5, //Glacier National Park
"PUQ" -> 5, //Punta Arenas
"SCR" -> 5, //Salzburg
"ASW" -> 5, //Abu Simbel Egypt
"MHH" -> 5, //Marsh Harbour Bahammas
"AEY" -> 5, //Thingvellir National Park Iceland
"BOB" -> 5, //Bora Bora French Polynesia
"MRE" -> 5, //Maasai Mara National Reserve Kenya
"SEU" -> 5,
"MFU" -> 5,
"YXY" -> 5, //Whitehorse
"GHB" -> 5, //Governor's Harbour Bahamas
"GGT" -> 5, //Bahamas
"ZSA" -> 5,
"CYB" -> 5, //West End
"HOR" -> 5, //Azores Horta
"HBE" -> 5, //Alexandria
    ),
    VACATION_HUB -> Map[String, Int](
"CJU" -> 190, //Jeju City
"CTS" -> 140, //Chitose / Tomakomai
"MEL" -> 94, //Melbourne
"MCO" -> 89, //Orlando
"YIA" -> 86, //Yogyakarta
"SYD" -> 84, //Sydney Australia
"JED" -> 80, //Jeddah
"TRD" -> 78, //Trondheim
"CUN" -> 75, //Cancún
"SYX" -> 75, //Sanya
"HAK" -> 75, //Haikou
"HNL" -> 70, //Honolulu
"PMI" -> 70, //Palma De Mallorca
"LAS" -> 70, //Las Vegas
"KRR" -> 70, //Krasnodar
"OKA" -> 70, //Naha
"CTG" -> 68, //Cartagena
"FLN" -> 67, //Florianópolis
"SXR" -> 67, //Srinagar
"VNS" -> 66, //Varanasi
"AGP" -> 65, //Málaga
"RHO" -> 60, //Rodes Island
"AER" -> 60, //Sochi
"SJD" -> 60, //San José del Cabo
"MFM" -> 60, //Macau
"PKX" -> 60, //Beijing China
"PMC" -> 60, //Puerto Montt
"PMV" -> 60, //Isla Margarita
"PVR" -> 59, //Puerto Vallarta
"OGG" -> 58, //Kahului
"OOL" -> 57, //Gold Coast
"CGH" -> 55, //São Paulo
"SAW" -> 52, //Istanbul
"BAH" -> 51, //Manama
"MHD" -> 51, //Mashhad
"AYT" -> 50, //Antalya
"BKI" -> 50, //Kota Kinabalu
"LPA" -> 50, //Gran Canaria Island
"TFS" -> 50, //Tenerife Island
"FAO" -> 50, //Faro
"BGO" -> 50, //Bergen
"GRU" -> 49, //São Paulo
"BNE" -> 49, //Brisbane
"ORY" -> 49, //Paris
"KOS" -> 48, //Sihanukville
"BNA" -> 48, //Nashville
"MLA" -> 48, //Valletta
"MSY" -> 47, //New Orleans
"OLB" -> 47, //Olbia (SS)
"PUJ" -> 45, //Punta Cana
"DMK" -> 45, //Bangkok
"KUL" -> 45, //Kuala Lumpur
"POA" -> 45, //Porto Alegre
"HBA" -> 45, //Hobart
"BAR" -> 45, //Qionghai
"KIH" -> 45, //Kish Island IR
"GIG" -> 44, //Rio De Janeiro
"GOI" -> 44, //Vasco da Gama
"LGA" -> 44, //New York
"AEP" -> 42, //Buenos Aires
"CAG" -> 42, //Cagliari
"CTA" -> 42, //Catania
"TOS" -> 42, //Tromsø
"GRO" -> 41, //Girona
"CNS" -> 40, //Cairns
"PMO" -> 40, //Palermo
"DEL" -> 40,
"KZN" -> 40, //Kazan
"ITM" -> 40, //Osaka Japan
"OKD" -> 40, //Sapporo
"BTH" -> 39, //Batam Island
"REC" -> 39, //Recife
"VIX" -> 39, //Vitória
"SHA" -> 39, //Shanghai China
"MAD" -> 38, //Madrid
"VAR" -> 38, //Varna
"MRS" -> 38, //Marseille
"CPT" -> 36, //Cape Town
"FLR" -> 36, //Firenze
"AKL" -> 36, //Auckland
"BPS" -> 36, //Porto Seguro
"TSV" -> 36, //Townsville
"BKK" -> 35, //Bangkok
"YVR" -> 35, //Vancouver
"LVI" -> 35, //Livingstone
"STT" -> 35, //Charlotte Amalie
"PER" -> 35, //Perth
"MED" -> 35, //Medina
"DBV" -> 35, //Dubrovnik
"HAN" -> 35, //Hanoi
"CCJ" -> 35, //Calicut
"ALC" -> 35, //Alicante
"KWL" -> 35, //Guilin City
"LYS" -> 35, //Lyon
"CNF" -> 35, //Belo Horizonte
"HGH" -> 34, //Hangzhou
"GDN" -> 34, //GdaÅ„sk
"NQN" -> 34, //Neuquen
"TAO" -> 34, //Qingdao
"MBJ" -> 32, //Montego Bay
"IGU" -> 32, //Foz Do IguaÃ§u
"BOS" -> 32,
"HRG" -> 30, //Hurghada
"SSH" -> 30, //Sharm el-Sheikh
"CEB" -> 30, //Lapu-Lapu City
"SJU" -> 30, //San Juan
"MPH" -> 30, //Malay
"PSA" -> 30, //Pisa
"CFU" -> 30, //Kerkyra Island
"COK" -> 30, //Kochi
"IKA" -> 30, //Tehran
"VKO" -> 30, //Moscow
"THR" -> 30, //Tehran
"CJC" -> 30, //Calama
"ADZ" -> 30, //San Andrés
"CTU" -> 30, //Chengdu
"DCA" -> 30, //Washington
"VLC" -> 30, //Valencia
"RAK" -> 29, //Marrakech
"IGR" -> 29, //Puerto Iguazu
"FOR" -> 29, //Fortaleza
"RNO" -> 29, //Reno
"KOA" -> 28, //Kailua-Kona
"MAA" -> 28, //Chennai
"RSW" -> 27, //Fort Myers
"REU" -> 27, //Reus
"SLL" -> 27, //Salalah
"HER" -> 26, //Heraklion
"SKD" -> 26, //Samarkand
"FLL" -> 26, //Miami
"MYR" -> 26, //Myrtle Beach
"SDU" -> 26, //Rio De Janeiro
"PXO" -> 26, //Peneda-Gerês National Park Portugal
"LIS" -> 25, //Lisbon
"IBZ" -> 25, //Ibiza
"TFN" -> 25, //Tenerife Island
"VFA" -> 25, //Victoria Falls
"HIJ" -> 25, //Hiroshima
"KMQ" -> 25, //Kumamoto
"BWI" -> 25, //Washington
"LIN" -> 25, //Milan Italian Alps
"KGS" -> 24, //Kos Island
"ACE" -> 24, //Lanzarote Island
"NAS" -> 24, //Nassau
"CHQ" -> 24, //Heraklion
"CIA" -> 24, //Ostia Antica Italy
"BWN" -> 24, //Bandar Seri Begawan
"HAM" -> 24, //Hamburg
"AJA" -> 24, //Ajaccio/NapolÃ©on Bonaparte
"BIA" -> 24, //Bastia-Poretta
"NVT" -> 24, //Navegantes
"STI" -> 24, //Santiago
"TPA" -> 24, //Tampa
"YYC" -> 24, //Calgary
"SDQ" -> 24, //Santo Domingo
"JAI" -> 22, //Jaipur
"BRC" -> 22, //San Carlos de Bariloche
"SMR" -> 22, //Santa Marta
"IKT" -> 22, //Irkutsk
"SIP" -> 22, //Simferopol
"TFU" -> 22, //Chengdu
"JMK" -> 21, //Mykonos Island
"SSA" -> 21, //Salvador
"BUF" -> 21, //Buffalo
"CTM" -> 21, //Chetumal
"HTI" -> 21, //Hamilton Island Resort
"MID" -> 21, //Mérida
"IXB" -> 21, //Bagdogra Darjeeling
"DAD" -> 20, //Da Nang
"BCN" -> 20, //Barcelona
"SCL" -> 20, //Santiago
"NCE" -> 20, //Nice
"BER" -> 20, //Berlin
"SAI" -> 20, //Siem Reap
"FUE" -> 20, //Fuerteventura Island
"NAP" -> 20, //Nápoli
"JTR" -> 20, //Santorini Island
"SVO" -> 20, //Moscow
"KRK" -> 20, //Kraków
"LED" -> 20, //St. Petersburg
"LIR" -> 20, //Liberia Costa Rica
"GYD" -> 20, //Baku
"UVF" -> 20, //Vieux Fort
"XIY" -> 20, //Terracotta Army China
"BRI" -> 20, //Bari
"PNQ" -> 20, //Pune
"KNH" -> 20, //Kinmen
"NKG" -> 20, //Nanjing
"RUH" -> 20,
"FNC" -> 19, //Funchal
"AMM" -> 19, //Amman
"LIH" -> 19, //Lihue
"AMD" -> 19, //Ahmedabad
"ZTH" -> 18, //Zakynthos Island
"YUL" -> 18, //Montreal
"NBO" -> 18, //Nairobi
"ITO" -> 18, //Hilo
"ANU" -> 18, //St. John's
"EFL" -> 18, //Kefallinia Island
"IOS" -> 18, //IlhÃ©us
"RVN" -> 18, //Rovaniemi
"CGB" -> 18, //Cuiabá
"DLC" -> 18, //Dalian
"PHL" -> 18,
"INN" -> 18, //Innsbruck
"KIN" -> 17, //Kingston
"EYW" -> 17, //Key West
"FTE" -> 17, //El Calafate
"SHJ" -> 17, //Dubai
"IXC" -> 17, //Chandigarh
"FOC" -> 17, //Fuzhou
"HUX" -> 16, //Huatulco
"MAH" -> 16, //Menorca Island
"GCM" -> 16, //Georgetown
"LLA" -> 16, //LuleÃ¥
"PPP" -> 16, //Whitsunday Coast Airport
"YQB" -> 16, //Quebec
"BJV" -> 15, //Bodrum
"PEN" -> 15, //Penang
"IAD" -> 15, //Washington
"NLU" -> 15, //Mexico City
"LCA" -> 15, //Larnarca
"TIV" -> 15, //Tivat
"JNB" -> 15, //Johannesburg
"TGZ" -> 15, //Tuxtla Gutiérrez
"SPU" -> 15, //Split
"PLS" -> 15, //Providenciales Turks and Caicos
"ASP" -> 15, //Alice Springs
"ZIA" -> 15, //Moscow
"BLQ" -> 15, //Bologna
"SNA" -> 15, //Santa Ana
"YYT" -> 15, //St John
"XMN" -> 15, //Xiamen
"ISG" -> 15, //Ishigaki JP
"POP" -> 14, //Puerto Plata Dominican Republic
"BOJ" -> 14, //Burgas
"EDI" -> 14, //Edinburgh
"BME" -> 14, //Broome
"OTP" -> 14, //Bucharest
"YHZ" -> 14, //Halifax
"CWB" -> 14, //Curitiba
"VBY" -> 14, //Visby, SE
"LPQ" -> 12, //Luang Phabang
"FAT" -> 12, //Yosemite National Park USA
"NGO" -> 12, //Tokoname
"AGX" -> 12, //Agatti
"BAQ" -> 12, //Barranquilla
"BDS" -> 12, //Brindisi
"FEN" -> 12, //Fernando De Noronha
"KTN" -> 12, //Ketchikan
"TSN" -> 12, //Tianjin
"ATQ" -> 12, //Amritsar
"BSB" -> 12, //Brasília
"LKO" -> 12, //Lucknow
"SHE" -> 12, //Shenyang
"VCP" -> 12, //Campinas
"SBZ" -> 12, //Sibiu
"BZN" -> 11, //Bozeman
"FUK" -> 11, //Fukuoka
"VOG" -> 11, //Volgograd
"GYN" -> 11, //Goiânia
"AMS" -> 10, //Amsterdam
"LAP" -> 10, //La Paz
"MUB" -> 10, //Maun
"PLZ" -> 10, //Addo Elephant National Park South Africa
"GCN" -> 10, //Grand Canyon
"STX" -> 10, //Christiansted
"AYQ" -> 10, //Ayers Rock
"UNA" -> 10, //Transamérica Resort Comandatuba Island
"GRQ" -> 10, //Grenoble French Alps
"FSZ" -> 10, //Fuji-Hakone-Izu National Park Japan
"BJL" -> 10, //Banjul
"FSC" -> 10, //Figari Sud-Corse
"MTJ" -> 10, //Montrose (Ski resort)
"HRB" -> 10, //Harbin
"CSX" -> 10, //Changsha
"ISG" -> 10, //Ishigaki
"SVG" -> 10,
"JAC" -> 9, //Jackson
"CHC" -> 9, //Christchurch
"TRN" -> 9, //Turin Italian Alps
"ASE" -> 9, //Aspen
"IXZ" -> 9, //Port Blair
"KTA" -> 9, //Blue Mountains National Park Australia
"YXC" -> 9, //Banff National Park Canada
"ZAD" -> 9, //Zemunik (Zadar)
"BTV" -> 9, //Burlington Stowe/Sugarbush Vermont USA
"SZG" -> 8, //Salzburg Austrian Alps
"SZG" -> 8, //Berchtesgaden National Park Germany
"CLY" -> 8, //Calvi-Sainte-Catherine
"SLZ" -> 8, //São Luís
"SUN" -> 8, //Hailey Sun Valley Idaho USA
"THE" -> 8, //Teresina
"GCI" -> 8, //Jersey
"JER" -> 8, //Guernsey
"SAN" -> 8, //San Diego USA
"HYA" -> 7, //Cape Cod
"YDF" -> 7, //Gros Morne National Park Canada
"ACK" -> 6, //Nantucket
"EGE" -> 6, //Vail/Beaver Creek Colorado USA
"YLW" -> 6, //Jasper National Park Canada
"HDS" -> 5, //Kruger National Park South Africa
"FAI" -> 5, //Fairbanks
"IPC" -> 5, //Isla De Pascua
"ECP" -> 5, //Panama City Beach
"YYJ" -> 5, //Yoho National Park Canada
"SGU" -> 5, //Zion National Park
"STS" -> 5, //Sedona
"BRW" -> 5, //Denali National Park USA
"CNY" -> 5, //Arches National Park USA
"ZUH" -> 5, //Zhuhai
"HDN" -> 5, //Hayden Steamboat Springs Colorado USA
"CLQ" -> 5, //Nevado de Colima National Park Mexico
"CCK" -> 5,
"XCH" -> 5,
"NLK" -> 5,
"SUV" -> 5,
"LDH" -> 5,
"CMF" -> 5, //Chambéry
"CPX" -> 5, //Culebra PR
"VQS" -> 5, //Vieques PR
"TVC" -> 5, //Traverse City
"YTY" -> 5, //Yangzhou
"HHH" -> 5, //Hilton Head Island
"MFR" -> 5, //Crater lake
"GPT" -> 5, //Gulf port
"AO1" -> 5, //Aogashima	JP"
 ),
    FINANCIAL_HUB -> Map[String, Int](
"SIN" -> 80, //Singapore
"JFK" -> 75, //New York
"HND" -> 75, //Tokyo
"LHR" -> 65, //London
"FRA" -> 65, //Frankfurt
"HKG" -> 60, //Hong Kong
"CDG" -> 60, //Paris
"MUC" -> 60, //Munich
"YYZ" -> 60, //Toronto
"ICN" -> 58, //Seoul
"EWR" -> 55, //New York
"DXB" -> 55, //Dubai
"PEK" -> 50, //Beijing
"JNB" -> 48, //Johannesburg
"ORD" -> 45, //Chicago
"TPE" -> 45, //Taipei
"AMS" -> 45, //Amsterdam
"STR" -> 45, //Stuttgart
"BRU" -> 45, //Brussels
"KUL" -> 44, //Kuala Lumpur
"GVA" -> 44, //Geneva
"SZX" -> 42, //Shenzhen
"AUH" -> 42, //Abu Dhabi
"LAX" -> 40, //Los Angeles
"DUB" -> 40, //Dublin
"BER" -> 40, //Berlin
"ZRH" -> 40, //Zurich
"GRU" -> 40, //Sao Paulo
"SYD" -> 39, //Sydney
"LGW" -> 36, //London
"MAD" -> 36, //Madrid
"CAN" -> 36, //Guangzhou
"PVG" -> 36, //Shanghai
"BOS" -> 35, //Boston
"DFW" -> 35, //Dallas Fort Worth
"LGA" -> 35, //New York
"YVR" -> 35, //Vancouver
"BOM" -> 35, //Mumbai
"ARN" -> 34, //Stockholm
"DOH" -> 34, //Doha
"CPH" -> 34, //Copenhagen
"HAM" -> 34, //Hamburg
"MEL" -> 34, //Melbourne
"SFO" -> 34, //San Francisco
"OSL" -> 34, //Oslo
"KWI" -> 33, //Kuwait City
"TLV" -> 33, //Tel Aviv
"SCL" -> 33, //Santiago
"YUL" -> 32, //Montreal
"VIE" -> 32, //Vienna
"DME" -> 31, //Moscow
"LUX" -> 30, //Luxembourg
"ITM" -> 30, //Osaka
"KIX" -> 30, //Osaka
"SVO" -> 29, //Moscow
"EDI" -> 29, //Edinburgh
"IST" -> 28, //Istanbul
"PUS" -> 28, //Busan
"DEL" -> 28, //New Delhi
"MXP" -> 28, //Milan
"BOG" -> 26, //Bogota
"EZE" -> 26, //Buenos Aires
"CPT" -> 25, //Cape Town
"ORY" -> 25, //Paris
"PKX" -> 25, //Beijing
"YYC" -> 25, //Calgary
"BAH" -> 25, //Bahrain
"MEX" -> 24, //Mexico City
"ATL" -> 24, //Atlanta
"CLT" -> 24, //Charlotte
"GMP" -> 24, //Seoul
"SHA" -> 24, //Shanghai
"LOS" -> 24, //Lagos
"FCO" -> 22, //Rome
"GIG" -> 22, //Rio de Janeiro
"RUH" -> 21, //Riyadh
"LIM" -> 21, //Lima
"NGO" -> 21, //Nagoya
"TAS" -> 20, //Tashkent
"IAD" -> 20, //Washington DC
"DEN" -> 20, //Denver
"BKK" -> 20, //Bangkok
"FUK" -> 20, //Fukuoka
"LIN" -> 20, //Milan
"BUD" -> 20, //Budapest
"LCY" -> 20, //London
"HAJ" -> 20, //Hanover
"WAW" -> 19, //Warsaw
"PRG" -> 19, //Prague
"DCA" -> 18, //Washington DC
"MSP" -> 18, //Minneapolis
"AKL" -> 18, //Auckland
"BLQ" -> 18, //Bologna
"BLR" -> 18, //Bangalore
"MAN" -> 18, //Manchester
"BCN" -> 18, //Barcelona
"BSB" -> 18, //Brasilia
"SGN" -> 18, //Ho Chi Minh City
"CGK" -> 18, //Jakarta
"JED" -> 17, //Jeddah
"MNL" -> 17, //Manila
"OAK" -> 16, //San Francisco
"HYD" -> 16, //Hyderabad
"TLL" -> 16, //Tallinn
"DUS" -> 16, //Dusseldorf
"CGN" -> 16, //Cologne
"MDW" -> 15, //Chicago
"IAH" -> 15, //Houston
"MIA" -> 15, //Miami
"SEA" -> 15, //Seattle
"PHX" -> 15, //Phoenix
"SJC" -> 15, //San Francisco
"DAL" -> 15, //Dallas
"DTW" -> 15, //Detroit
"RTM" -> 15, //The Hague
"RMO" -> 15, //Chisinau
"HEL" -> 15, //Helsinki
"CGH" -> 15, //Sao Paulo
"PHL" -> 14, //Philadelphia
"KHH" -> 14, //Kaohsiung
"TRN" -> 14, //Turin
"IKA" -> 14, //Tehran
"AMD" -> 14, //GIFT City-Gujarat
"PER" -> 14, //Perth
"BNE" -> 14, //Brisbane
"ALA" -> 13, //Almaty
"TLS" -> 13, //Toulouse
"VNO" -> 12, //Vilnius
"ALG" -> 12, //Algiers
"LEJ" -> 12, //Leipzig
"TAO" -> 12, //Qingdao
"TSN" -> 12, //Tianjin
"CBR" -> 12, //Canberra
"CMN" -> 11, //Casablanca
"KUN" -> 11, //Kaunas
"DAC" -> 11, //Dhaka
"BGI" -> 11, //Bridgetown
"HAN" -> 11, //Hanoi
"TSA" -> 10, //Taipei
"DMK" -> 10, //Bangkok
"LED" -> 10, //St Petersburg
"RIX" -> 10, //Riga
"MAA" -> 10, //Chennai
"SDU" -> 10, //Rio de Janeiro
"AEP" -> 10, //Buenos Aires
"NBO" -> 9, //Nairobi
"PTY" -> 9, //Panama City
"BWI" -> 9, //Baltimore
"BGO" -> 9, //Bergen
"MLA" -> 9, //Malta
"ATH" -> 9, //Athens
"TFU" -> 9, //Chengdu
"MDE" -> 9, //Medellin
"BEG" -> 9, //Belgrade
"AUS" -> 8, //Austin
"SAN" -> 8, //San Diego
"BTS" -> 8, //Bratislava
"GOT" -> 8, //Gothenburg
"OTP" -> 8, //Bucharest
"KHI" -> 8, //Karachi
"CCU" -> 8, //Kolkata
"CKG" -> 8, //Jakarta
"NCL" -> 8, //Newcastle
"LYS" -> 8, //Grenoble
"LCA" -> 8, //Nicosia
"NKG" -> 8, //Nanjing
"CTU" -> 8, //Chengdu
"BDA" -> 8, //Bermuda
"ADL" -> 8, //Adelaide
"DUR" -> 8, //Durban
"NLU" -> 8, //Mexico City
"PDX" -> 7, //Portland
"PIT" -> 7, //Pittsburgh
"NQZ" -> 7, //Nur-Sultan
"DLC" -> 7, //Dalian
"GYD" -> 7, //Baku
"PNQ" -> 7, //Pune
"AAL" -> 6, //Aalborg
"HGH" -> 6, //Hangzhou
"NAS" -> 6, //Nassau
"SOF" -> 6, //Sofia
"BNA" -> 5, //Nashville
"SMF" -> 5, //Sacramento
"ANC" -> 5, //Anchorage
"JNU" -> 5, //Juneau
"DMM" -> 5, //
"AHB" -> 5, //
"KGL" -> 5, //Kigali
"WLG" -> 5, //Wellington
"TRD" -> 5, //Trondheim
"ABV" -> 5, //
"MTY" -> 5, //Monterrey
"KEF" -> 5, //Reykjavik
"IOM" -> 5, //Castletown
"GLA" -> 5, //Glasgow
"BHX" -> 5, //Birmingham
"ADD" -> 5, //Addis Ababa
"DTM" -> 5, //Dortmund
"CLO" -> 5, //Cali
"XIY" -> 5, //Xi'an
"YQB" -> 5, //Quebec City
"MBA" -> 5, //Mombasa
"JRB" -> 1, //NYC Heliport
"JRA" -> 1, //NYC Heliport
"TSS" -> 1, //NYC Heliport
"SW1" -> 1, //Rio Heliport
"CB7" -> 1, //Vancouver Heliport
"HHP" -> 1, //Hong Kong
"GR0" -> 1, //Sao Paulo
    ), 
    DOMESTIC_AIRPORT -> Map(
      "LGA" -> 0,
      "DCA" -> 0,
      "MDW" -> 0,
      "SNA" -> 0,
      "BUR" -> 0,
      "OAK" -> 0,
      "DAL" -> 0,
      "HOU" -> 0,
      "AZA" -> 0,
      "COS" -> 0,
      "PAE" -> 0,
      "PIE" -> 0,
      "SFB" -> 0,
      "USA" -> 0,
      "PGD" -> 0,
      "LIH" -> 0,
      "OGG" -> 0,
      "AKN" -> 0,
      "ORH" -> 0,
      "SIG" -> 0,
      //canada
      "YTZ" -> 0,
      "YHU" -> 0,
      "YFB" -> 0,
      //mexico
      "TLC" -> 0,
      "CJS" -> 0,
      //EU
      "EIN" -> 0,
      "CRL" -> 0,
      "ANR" -> 0,
      "BVA" -> 0,
      "HHN" -> 0,
      "BRE" -> 0,
      "DTM" -> 0,
      "FMM" -> 0,
      "FAO" -> 0,
      "REU" -> 0,
      "GRO" -> 0,
      "LIN" -> 0,
      "CIA" -> 0,
      "TSF" -> 0,
      "NYO" -> 0,
      "BMA" -> 0,
      "TRF" -> 0,
      "WMI" -> 0,
      //GB
      "BHD" -> 0,
      //iceland
      "RKV" -> 0,
      //china
      "CTU" -> 0,
      "PKX" -> 0,
      "SHA" -> 0,
      "ZUH" -> 0,
      "LXA" -> 0,
      //japan
      "ITM" -> 0,
      "UKB" -> 0,
      "IBR" -> 0,
      "OKD" -> 0,
      //argentina
      "AEP" -> 0,
      //brazil
      "CGH" -> 0,
      "SDU" -> 0,
      //colombia
      "EOH" -> 0,
      "FLA" -> 0,
      //chile
      "LSC" -> 0,
      //dominican-republic
      "JBQ" -> 0,
      //belize
      "TZA" -> 0,
      //iran
      "THR" -> 0,
      "PGU" -> 0,
      "ABD" -> 0,
      "KIH" -> 0,
      "AWZ" -> 0,
      //india
      "HDO" -> 0,
      "DHM" -> 0,
      "BDQ" -> 0,
      "PNY" -> 0,
      "AIP" -> 0,
      "STV" -> 0,
      "KNU" -> 0,
      //russia
      "CEK" -> 0,
      "KEJ" -> 0,
      "BTK" -> 0,
      "YKS" -> 0,
      //southern africa
      "HLA" -> 0,
      "ERS" -> 0,
      //indonesia
      "HLP" -> 0,
      //Australia
      "AVV" -> 0,
      "MCY" -> 0,
      "LST" -> 0
    )
  ) + (GATEWAY_AIRPORT -> getGatewayAirports().map(iata => (iata, 0)).toMap) + (ELITE_CHARM -> getEliteDestinations())

  patchFeatures()

  def patchFeatures() = {
    val airportFeatures = scala.collection.mutable.Map[String, ListBuffer[AirportFeature]]()
    featureList.foreach {
      case (featureType, airportMap) =>
        airportMap.foreach {
          case (airportIata, featureStrength) =>
            val featuresForThisAirport = airportFeatures.getOrElseUpdate(airportIata, ListBuffer[AirportFeature]())
            featuresForThisAirport += AirportFeature(featureType, featureStrength)
        }
    }


    airportFeatures.toList.foreach {
        case (iata, features) =>
          AirportSource.loadAirportByIata(iata) match {
            case Some(airport) =>
              AirportSource.updateAirportFeatures(airport.id, features.toList)
            case None =>
              println(s"Cannot find airport with iata $iata to patch $features")
          }
      }
      IsolatedAirportPatcher.patchIsolatedAirports()
  }

    def getEliteDestinations() : Map[String, Int] = {
      val destinations = DestinationSource.loadAllDestinations()
      val iataMap = destinations.groupBy(_.airport.iata).view.mapValues(_.length).toMap
      println("inserting elite destinations to features...")
      println(iataMap)
      iataMap
    }


  def getGatewayAirports() : List[String] = {
    //The most powerful airport of every country
    val airportsByCountry = AirportSource.loadAllAirports().groupBy(_.countryCode).filter(_._2.length > 0)
    val topAirportByCountry = airportsByCountry.view.mapValues(_.sortBy(_.power).last)

    val baseList = topAirportByCountry.values.map(_.iata).toList

    val list: mutable.ListBuffer[String] = collection.mutable.ListBuffer(baseList:_*)

    list -= "HND"
    list -= "CGO" //China
    list -= "OSS" //Uzbekistan
    list += "FRU"
    list -= "LHE" //Pakistan
    list += "ISB"
    list -= "GYE" //Ecuador
    list += "UIO"
    list -= "THR" //Iran
    list += "IKA"
    list -= "RUH" //Saudi
    list += "JED"
    list -= "OND" //Nambia
    list += "WDH"
    list -= "ZND" //Mali
    list += "NIM"
    list -= "BYK" //Ivory Coast
    list += "ABJ"
    list -= "DLA" //Cameroon
    list += "NSI"
    list -= "MQQ" //Chad
    list += "NDJ"
    list -= "BLZ" //Malawi
    list += "LLW"
    list -= "KGA" //DRC
    list -= "MJM"
    list += "FIH"
    list -= "KAN" //Nigeria
    list += "LOS"
    list -= "APL" //Mozambique
    list += "MPM"
    list -= "MWZ" //Tanzania
    list += "DAR"
    list -= "HGU" //Tanzania
    list += "POM"
    list -= "STX" //US VI
    list += "STT"
    list -= "XSC" //
    list += "PLS"


    //now add extra ones for bigger countries
    //from top to bottom by pop coverage, so we wouldnt miss any
    list.appendAll(List(
      "SP1", //South Pole station
      "CAN", //China
      "PVG",
      "PEK",
      "JFK", //US
      "LAX",
      "SFO",
      "MIA",
      "BOM", //India
      "RUH", //Saudi
      "AUH", //UAE
      "AYT", //Turkey
      "CPT", //South Africa
      "GIG", //Brazil
      "GRU",
      "NRT", //Japan
      "KIX",
      "SVO", //Russia
      "LED",
      "FCO", //Italy
      "MXP",
      "MAD", //Spain
      "BCN",
      "FRA", //Germany
      "MUC",
      "SYD", //Australia
      "MEL",
      "YVR", //Canada
      "YUL",
      "YYZ"))
    list.toList
  }
}
