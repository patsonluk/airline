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
"CNX" -> 110, //Chiang Mai
"HKT" -> 109, //Phuket
"CXR" -> 96, //Nha Trang
"KBV" -> 84, //Krabi
"PQC" -> 82, //Phu Quoc Island
"IST" -> 80, //Istanbul
"CDG" -> 80, //Paris
"LGK" -> 76, //Langkawi
"AYT" -> 75, //Antalya
"LHR" -> 75, //London
"DPS" -> 74, //Denpasar-Bali Island
"BKK" -> 66, //Bangkok
"DXB" -> 65, //Dubai
"CUZ" -> 65, //Cusco
"HKG" -> 63, //Hong Kong
"PNH" -> 62, //Phnom Penh
"MLE" -> 55, //Malé Maldives
"JFK" -> 54, //New York
"NRT" -> 53, //Tokyo / Narita
"CMB" -> 48, //Colombo
"DMK" -> 45, //Bangkok
"KUL" -> 43, //Kuala Lumpur
"CEB" -> 42, //Lapu-Lapu City
"SIN" -> 42, //Singapore
"VCE" -> 42, //Venice
"MIA" -> 42, //Miami
"AMS" -> 41, //Amsterdam
"LAX" -> 41, //Los Angeles
"CUN" -> 40, //Cancún
"MBJ" -> 40, //Montego Bay
"DAD" -> 40, //Da Nang
"KIX" -> 40, //Osaka
"RUN" -> 40, //St Denis
"GRU" -> 39, //São Paulo
"VIE" -> 39, //Vienna
"ATH" -> 39, //Athens
"MCO" -> 38, //Orlando
"EWR" -> 38, //New York City USA
"LGW" -> 38, //London United Kingdom
"HND" -> 38, //Tokyo / Narita
"BKI" -> 37, //Kota Kinabalu
"BOM" -> 37, //Mumbai
"PUJ" -> 36, //Punta Cana
"MAD" -> 36, //Madrid
"CPT" -> 35, //Cape Town
"HRG" -> 34, //Hurghada
"NCE" -> 34, //Nice
"YYZ" -> 34, //Toronto Canada
"BER" -> 33, //Berlin
"UTP" -> 32, //Rayong
"LOP" -> 32, //Mataram
"HNL" -> 31, //Honolulu
"SAI" -> 31, //Siem Reap
"AGP" -> 30, //MÃ¡laga
"SYD" -> 30, //Sydney Australia
"JED" -> 30, //Jeddah
"AEP" -> 30, //Buenos Aires
"USM" -> 30, //Na Thon (Ko Samui Island)
"PEK" -> 29, //Beijing
"MXP" -> 29, //Milan
"DUB" -> 29, //Dublin Ireland
"CAI" -> 29, //Cairo Egypt
"MUC" -> 29, //Munich
"MEX" -> 29, //Mexico City
"BCN" -> 28, //Barcelona
"MPH" -> 28, //Malay
"PEN" -> 28, //Penang
"PPT" -> 28, //Papeete
"AUA" -> 28, //Oranjestad
"ICN" -> 28, //Seoul
"FAO" -> 27, //Faro
"SSH" -> 27, //Sharm el-Sheikh
"VRA" -> 27, //Varadero
"SCL" -> 26, //Santiago
"LIS" -> 26, //Lisbon
"TLV" -> 26, //Tel Aviv
"BUD" -> 26, //Budapest
"KEF" -> 26, //Reykjavík
"BAH" -> 25, //Manama
"GOI" -> 25, //Vasco da Gama
"YVR" -> 25, //Vancouver
"LCY" -> 25, //London United Kingdom
"RHO" -> 24, //Rodes Island
"PRG" -> 24, //Prague
"LAP" -> 24, //La Paz
"OSL" -> 24, //Oslo
"SPX" -> 24, //Cairo
"NOU" -> 24, //Nouméa
"PTP" -> 24, //Pointe-Ã -Pitre
"PMO" -> 23, //Palermo
"GUM" -> 23, //Hagåtña Guam International Airport
"KTM" -> 23, //Kathmandu
"CPH" -> 23, //Copenhagen
"SXM" -> 22, //Saint Martin
"ORY" -> 22, //Paris
"MLA" -> 22, //Valletta
"MRU" -> 22, //Port Louis
"EZE" -> 22, //Buenos Aires
"RAK" -> 21, //Marrakech
"BNE" -> 21, //Brisbane
"NAP" -> 21, //Nápoli
"IAD" -> 21, //Washington
"TRV" -> 21, //Thiruvananthapuram
"LXA" -> 21, //Lhasa
"SFO" -> 21, //San Francisco
"MQP" -> 21, //Mpumalanga
"SEZ" -> 21, //Mahe Island
"GAN" -> 20, //Maldives
"GIG" -> 20, //Rio De Janeiro
"HAV" -> 20, //Havana
"HER" -> 20, //Heraklion
"EDI" -> 20, //Edinburgh
"LPQ" -> 20, //Luang Phabang
"LIN" -> 20, //Milan Italian Alps
"CUR" -> 20, //Willemstad
"NAN" -> 20, //Nadi
"POA" -> 20, //Porto Alegre
"MEL" -> 19, //Melbourne
"LAS" -> 19, //Las Vegas
"IBZ" -> 19, //Ibiza
"SJO" -> 19, //San Jose
"ZQN" -> 19, //Queenstown
"ZRH" -> 19, //Zurich
"EBB" -> 19, //Kampala
"AKL" -> 18, //Auckland
"PSA" -> 18, //Pisa
"DRW" -> 18, //Darwin
"MVD" -> 18, //Montevideo
"BGI" -> 18, //Bridgetown
"BVC" -> 18, //Rabil
"HEL" -> 18, //Helsinki
"MIR" -> 18, //Monastir
"PVG" -> 18, //Shanghai
"ORD" -> 18, //Chicago
"OGG" -> 17, //Kahului
"ASR" -> 17, //Kayseri
"STT" -> 17, //Charlotte Amalie
"CFU" -> 17, //Kerkyra Island
"SVO" -> 17, //Moscow
"LCA" -> 17, //Larnarca
"NBE" -> 17, //Enfidha
"RTB" -> 17, //Roatan Island
"ATL" -> 17,
"LIM" -> 17,
"GPS" -> 17, //Baltra Galapagos
"CNS" -> 16, //Cairns
"MRS" -> 16, //Marseille
"ARN" -> 16, //Stockholm
"MUB" -> 16, //Maun
"FDF" -> 16, //Fort-de-France
"WAW" -> 16, //Warsaw
"BSL" -> 16, //Mulhouse French/Swiss Alps
"BOG" -> 16,
"CAG" -> 15, //Cagliari
"BJV" -> 15, //Bodrum
"FLR" -> 15, //Firenze
"IKA" -> 15, //Tehran
"JMK" -> 15, //Mykonos Island
"ADB" -> 15, //Izmir
"BOD" -> 15, //prehistoric caves France
"FLG" -> 15, //Flagstaff Grand Canyon
"SJD" -> 14, //San José del Cabo
"PVR" -> 14, //Puerto Vallarta
"PER" -> 14, //Perth
"JRO" -> 14, //Arusha
"NAS" -> 14, //Nassau
"KRK" -> 14, //Kraków
"AMM" -> 14, //Amman
"SID" -> 14, //Espargos
"SGN" -> 14,
"MED" -> 13, //Medina
"LED" -> 13, //St. Petersburg
"ZNZ" -> 13, //Zanzibar
"DOH" -> 13,
"KOS" -> 12, //Sihanukville
"BTH" -> 12, //Batam Island
"LVI" -> 12, //Livingstone
"SJU" -> 12, //San Juan
"THR" -> 12, //Tehran
"PLZ" -> 12, //Addo Elephant National Park South Africa
"TBS" -> 12, //Tbilisi
"DME" -> 12, //Moscow
"RAI" -> 12, //Praia
"LBJ" -> 12, //Komodo National Park Indonesia
"VTE" -> 12, //Luang Prabang Laos
"SLC" -> 12, //Salt Lake City
"PTY" -> 12,
"VFA" -> 11, //Victoria Falls
"JAI" -> 11, //Jaipur
"LIR" -> 11, //Liberia Costa Rica
"ANC" -> 11, //Anchorage
"CZM" -> 10, //Cozumel
"HGH" -> 10, //Hangzhou
"IGU" -> 10, //Foz Do IguaÃ§u
"SAW" -> 10, //Istanbul
"CJC" -> 10, //Calama
"VKO" -> 10, //Moscow
"CHQ" -> 10, //Heraklion
"CIA" -> 10, //Ostia Antica Italy
"GYD" -> 10, //Baku
"FNC" -> 10, //Funchal
"PLS" -> 10, //Providenciales Turks and Caicos
"BON" -> 10, //Kralendijk Bonaire
"CCC" -> 10, //Cayo Coco
"PFO" -> 10, //Paphos
"SRQ" -> 10, //Sarasota/Bradenton
"DEN" -> 10,
"AUH" -> 10,
"CGK" -> 10,
"TPE" -> 10,
"SEA" -> 10,
"GND" -> 9,
"REC" -> 9, //Recife
"TGZ" -> 9, //Tuxtla GutiÃ©rrez
"NBO" -> 9, //Nairobi
"ECN" -> 9, //Nicosia
"LIF" -> 9, //Lifou
"GUA" -> 9, //Tikal Guatemala
"LJU" -> 9, //Triglav National Park Slovenia
"MSY" -> 8, //New Orleans
"CTA" -> 8, //Catania
"CCJ" -> 8, //Calicut
"BWN" -> 8, //Bandar Seri Begawan
"SMR" -> 8, //Santa Marta
"SSA" -> 8, //Salvador
"UVF" -> 8, //Vieux Fort
"STX" -> 8, //Christiansted
"SZG" -> 8, //Salzburg Austrian Alps
"USH" -> 8, //Ushuahia
"UPN" -> 8, //Kgalagadi Transfrontier Park South Africa/Botswana
"KGS" -> 7, //Kos Island
"SKD" -> 7, //Samarkand
"SZG" -> 7, //Berchtesgaden National Park Germany
"TNM" -> 7, //AQ
"BTS" -> 7, //Devin Castle Slovakia
"IGR" -> 6, //Puerto Iguazu
"GCN" -> 6, //Grand Canyon
"LXR" -> 6, //Luxor
"PNT" -> 6, //Torres del Paine National Park Chile
"BZE" -> 6, //Chiquibul National Park Belize
"XIY" -> 5, //Xi'an
"DBV" -> 5, //Dubrovnik
"JTR" -> 5, //Santorini Island
"KIN" -> 5, //Kingston
"DJE" -> 5, //Djerba
"XIY" -> 5, //Terracotta Army China
"ASP" -> 5, //Alice Springs
"FAT" -> 5, //Yosemite National Park USA
"HDS" -> 5, //Kruger National Park South Africa
"AYQ" -> 5, //Ayers Rock
"UNA" -> 5, //Transamérica Resort Comandatuba Island
"BZN" -> 5, //Bozeman
"FCA" -> 5, //Glacier National Park
"PUQ" -> 5, //Punta Arenas
"SCR" -> 5, //Salzburg
"ASW" -> 5, //Abu Simbel Egypt
"BRN" -> 5, //Bern Swiss Alps
"AEY" -> 5, //Thingvellir National Park Iceland
"BOB" -> 5, //Bora Bora French Polynesia
"MRE" -> 5, //Maasai Mara National Reserve Kenya
"SEU" -> 5,
"MFU" -> 5
    ),
    VACATION_HUB -> Map[String, Int](
"CJU" -> 170, //Jeju City
"CTS" -> 130, //Chitose / Tomakomai
"CUN" -> 90, //Cancún
"PMI" -> 90, //Palma De Mallorca
"MEL" -> 88, //Melbourne
"YIA" -> 86, //Yogyakarta
"SYD" -> 84, //Sydney Australia
"AGP" -> 80, //MÃ¡laga
"JED" -> 80, //Jeddah
"CTG" -> 80, //Cartagena
"AER" -> 75, //Sochi
"SYX" -> 75, //Sanya
"HNL" -> 70, //Honolulu
"LAS" -> 70, //Las Vegas
"OKA" -> 70, //Naha
"MCO" -> 69, //Orlando
"FLN" -> 67, //Florianópolis
"SXR" -> 67, //Srinagar
"VNS" -> 66, //Varanasi
"PUJ" -> 65, //Punta Cana
"HAK" -> 65, //Haikou
"RHO" -> 60, //Rodes Island
"SJD" -> 60, //San José del Cabo
"XIY" -> 60, //Xi'an
"MFM" -> 60, //Macau
"KRR" -> 60, //Krasnodar
"PKX" -> 60, //Beijing China
"PMC" -> 60, //Puerto Montt
"PMV" -> 60, //Isla Margarita
"PVR" -> 59, //Puerto Vallarta
"BNA" -> 58, //Nashville
"OOL" -> 57, //Gold Coast
"VAR" -> 56, //Varna
"COK" -> 55, //Kochi
"SAW" -> 52, //Istanbul
"BAH" -> 51, //Manama
"MHD" -> 51, //Mashhad
"AYT" -> 50, //Antalya
"BKI" -> 50, //Kota Kinabalu
"BCN" -> 50, //Barcelona
"FAO" -> 50, //Faro
"LPA" -> 50, //Gran Canaria Island
"TFS" -> 50, //Tenerife Island
"BGO" -> 50, //Bergen
"GRU" -> 49, //São Paulo
"BNE" -> 49, //Brisbane
"CGH" -> 49, //São Paulo
"OGG" -> 48, //Kahului
"KOS" -> 48, //Sihanukville
"GIG" -> 47, //Rio De Janeiro
"MSY" -> 47, //New Orleans
"OLB" -> 47, //Olbia (SS)
"DMK" -> 45, //Bangkok
"KUL" -> 45, //Kuala Lumpur
"CAG" -> 45, //Cagliari
"TRD" -> 45, //Trondheim
"HBA" -> 45, //Hobart
"BAR" -> 45, //Qionghai
"GOI" -> 44, //Vasco da Gama
"KZN" -> 44, //Kazan
"RMF" -> 44, //Marsa Alam
"AEP" -> 42, //Buenos Aires
"GRO" -> 41, //Girona
"HRG" -> 40, //Hurghada
"UTP" -> 40, //Rayong
"USM" -> 40, //Na Thon (Ko Samui Island)
"SCL" -> 40, //Santiago
"HAV" -> 40, //Havana
"POA" -> 40, //Porto Alegre
"CNS" -> 40, //Cairns
"BJV" -> 40, //Bodrum
"DBV" -> 40, //Dubrovnik
"LGA" -> 40, //New York
"ASR" -> 39, //Kayseri
"BTH" -> 39, //Batam Island
"REC" -> 39, //Recife
"HAM" -> 39, //Hamburg
"VIX" -> 39, //VitÃ³ria
"SHA" -> 39, //Shanghai China
"CTA" -> 38, //Catania
"JTR" -> 38, //Santorini Island
"FUE" -> 38, //Fuerteventura Island
"GZP" -> 38, //GazipaÅŸa
"MBJ" -> 36, //Montego Bay
"CPT" -> 36, //Cape Town
"AKL" -> 36, //Auckland
"FLR" -> 36, //Firenze
"BPS" -> 36, //Porto Seguro
"TSV" -> 36, //Townsville
"BKK" -> 35, //Bangkok
"YVR" -> 35, //Vancouver
"STT" -> 35, //Charlotte Amalie
"PER" -> 35, //Perth
"MED" -> 35, //Medina
"LVI" -> 35, //Livingstone
"SJU" -> 35, //San Juan
"CCJ" -> 35, //Calicut
"KGS" -> 35, //Kos Island
"IGR" -> 35, //Puerto Iguazu
"ALC" -> 35, //Alicante
"KWL" -> 35, //Guilin City
"LYS" -> 35, //Lyon
"CZM" -> 34, //Cozumel
"HGH" -> 34, //Hangzhou
"ACE" -> 34, //Lanzarote Island
"GDN" -> 34, //GdaÅ„sk
"KIH" -> 34, //Kish Island
"NQN" -> 34, //Neuquen
"TAO" -> 34, //Qingdao
"POP" -> 34, //Puerto Plata Dominican Republic
"TAO" -> 34, //Qingdao
"HER" -> 33, //Heraklion
"RSW" -> 33, //Fort Myers
"ZTH" -> 33, //Zakynthos Island
"IGU" -> 32, //Foz Do IguaÃ§u
"KIN" -> 32, //Kingston
"CNF" -> 31, //Belo Horizonte
"CEB" -> 30, //Lapu-Lapu City
"DAD" -> 30, //Da Nang
"MPH" -> 30, //Malay
"SSH" -> 30, //Sharm el-Sheikh
"OSL" -> 30, //Oslo
"PSA" -> 30, //Pisa
"CFU" -> 30, //Kerkyra Island
"IKA" -> 30, //Tehran
"JRO" -> 30, //Arusha
"THR" -> 30, //Tehran
"CJC" -> 30, //Calama
"VKO" -> 30, //Moscow
"TGZ" -> 30, //Tuxtla GutiÃ©rrez
"SKD" -> 30, //Samarkand
"JNB" -> 30, //Johannesburg
"ADZ" -> 30, //San AndrÃ©s
"CTU" -> 30, //Chengdu
"DCA" -> 30, //Washington
"TFN" -> 30, //Tenerife Island
"TIV" -> 30, //Tivat
"VLC" -> 30, //Valencia
"FOR" -> 29, //Fortaleza
"HUX" -> 29, //Huatulco
"MAH" -> 29, //Menorca Island
"RNO" -> 29, //Reno
"BOJ" -> 28, //Burgas
"KOA" -> 28, //Kailua-Kona
"MAA" -> 28, //Chennai
"YUL" -> 28, //Montreal
"BOS" -> 28,
"REU" -> 27, //Reus
"SLL" -> 27, //Salalah
"AGA" -> 26, //Agadir
"FLL" -> 26, //Miami
"MYR" -> 26, //Myrtle Beach
"SDU" -> 26, //Rio De Janeiro
"ORY" -> 25, //Paris
"IBZ" -> 25, //Ibiza
"MRS" -> 25, //Marseille
"JMK" -> 25, //Mykonos Island
"VFA" -> 25, //Victoria Falls
"BRC" -> 25, //San Carlos de Bariloche
"SPU" -> 25, //Split
"HIJ" -> 25, //Hiroshima
"KMQ" -> 25, //Kumamoto
"BWI" -> 25, //Washington
"NAS" -> 24, //Nassau
"CHQ" -> 24, //Heraklion
"CIA" -> 24, //Ostia Antica Italy
"BWN" -> 24, //Bandar Seri Begawan
"AJA" -> 24, //Ajaccio/NapolÃ©on Bonaparte
"BIA" -> 24, //Bastia-Poretta
"NVT" -> 24, //Navegantes
"PPS" -> 24, //Puerto Princesa City
"STI" -> 24, //Santiago
"TPA" -> 24, //Tampa
"YYC" -> 24, //Calgary
"MLA" -> 22, //Valletta
"JAI" -> 22, //Jaipur
"SMR" -> 22, //Santa Marta
"DJE" -> 22, //Djerba
"IKT" -> 22, //Irkutsk
"SIP" -> 22, //Simferopol
"TFU" -> 22, //Chengdu
"PXO" -> 22, //Peneda-Gerês National Park Portugal
"SSA" -> 21, //Salvador
"BUF" -> 21, //Buffalo
"CTM" -> 21, //Chetumal
"HTI" -> 21, //Hamilton Island Resort
"MID" -> 21, //Mérida
"IXB" -> 21, //Bagdogra Darjeeling
"NCE" -> 20, //Nice
"BER" -> 20, //Berlin
"SAI" -> 20, //Siem Reap
"PRG" -> 20, //Prague
"PMO" -> 20, //Palermo
"NAP" -> 20, //Nápoli
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
"DEL" -> 20,
"RUH" -> 20,
"EDI" -> 19, //Edinburgh
"AMM" -> 19, //Amman
"FNC" -> 19, //Funchal
"SDQ" -> 19, //Santo Domingo
"LIH" -> 19, //Lihue
"AMD" -> 19, //Ahmedabad
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
"EYW" -> 17, //Key West
"FTE" -> 17, //El Calafate
"SHJ" -> 17, //Dubai
"IXC" -> 17, //Chandigarh
"FOC" -> 17, //Fuzhou
"BGY" -> 16, //Milan
"GCM" -> 16, //Georgetown
"LLA" -> 16, //LuleÃ¥
"PPP" -> 16, //Whitsunday Coast Airport
"YQB" -> 16, //Quebec
"MAD" -> 15, //Madrid
"PEN" -> 15, //Penang
"IAD" -> 15, //Washington
"LCA" -> 15, //Larnarca
"ARN" -> 15, //Stockholm
"ZNZ" -> 15, //Zanzibar
"PLS" -> 15, //Providenciales Turks and Caicos
"ASP" -> 15, //Alice Springs
"BLQ" -> 15, //Bologna
"SNA" -> 15, //Santa Ana
"YYT" -> 15, //St John
"XMN" -> 15, //Xiamen
"HAN" -> 15, //Hanoi
"JAC" -> 14, //Jackson
"OTP" -> 14, //Bucharest
"YHZ" -> 14, //Halifax
"CWB" -> 14, //Curitiba
"LPQ" -> 12, //Luang Phabang
"FAT" -> 12, //Yosemite National Park USA
"HDS" -> 12, //Kruger National Park South Africa
"NGO" -> 12, //Tokoname
"JNU" -> 12, //Juneau
"AGX" -> 12, //Agatti
"BAQ" -> 12, //Barranquilla
"BDS" -> 12, //Brindisi
"FEN" -> 12, //Fernando De Noronha
"KTN" -> 12, //Ketchikan
"TSN" -> 12, //Tianjin
"ATQ" -> 12, //Amritsar
"BSB" -> 12, //Brasília
"HFE" -> 12, //Hefei
"LKO" -> 12, //Lucknow
"MAO" -> 12, //Manaus
"SHE" -> 12, //Shenyang
"VCP" -> 12, //Campinas
"WRE" -> 12, //Lake District National Park England
"FUK" -> 11, //Fukuoka
"BTV" -> 11, //Burlington Stowe/Sugarbush Vermont USA
"VOG" -> 11, //Volgograd
"GYN" -> 11, //Goiânia
"AMS" -> 10, //Amsterdam
"LIS" -> 10, //Lisbon
"LAP" -> 10, //La Paz
"MUB" -> 10, //Maun
"PLZ" -> 10, //Addo Elephant National Park South Africa
"STX" -> 10, //Christiansted
"GCN" -> 10, //Grand Canyon
"AYQ" -> 10, //Ayers Rock
"UNA" -> 10, //Transamérica Resort Comandatuba Island
"IPC" -> 10, //Isla De Pascua
"GRQ" -> 10, //Grenoble French Alps
"FSZ" -> 10, //Fuji-Hakone-Izu National Park Japan
"BJL" -> 10, //Banjul
"BME" -> 10, //Broome
"FSC" -> 10, //Figari Sud-Corse
"MTJ" -> 10, //Montrose (Ski resort)
"YZF" -> 10, //Yellowknife
"ZIA" -> 10, //Moscow
"HRB" -> 10, //Harbin
"CSX" -> 10, //Changsha
"OMA" -> 10, //Pico de Orizaba National Park Mexico
"DEL" -> 10,
"TRN" -> 9, //Turin Italian Alps
"ASE" -> 9, //Aspen
"IXZ" -> 9, //Port Blair
"KTA" -> 9, //Blue Mountains National Park Australia
"YXC" -> 9, //Banff National Park Canada
"ZAD" -> 9, //Zemunik (Zadar)
"SZG" -> 8, //Salzburg Austrian Alps
"SZG" -> 8, //Berchtesgaden National Park Germany
"FAI" -> 8, //Fairbanks
"CHC" -> 8, //Christchurch
"CLY" -> 8, //Calvi-Sainte-Catherine
"SLZ" -> 8, //São Luís
"SUN" -> 8, //Hailey Sun Valley Idaho USA
"THE" -> 8, //Teresina
"GCI" -> 8, //Jersey
"JER" -> 8, //Guernsey
"BZN" -> 7, //Bozeman
"HYA" -> 7, //Cape Cod
"YDF" -> 7, //Gros Morne National Park Canada
"ACK" -> 6, //Nantucket
"EGE" -> 6, //Vail/Beaver Creek Colorado USA
"YLW" -> 6, //Jasper National Park Canada
"YYJ" -> 5, //Yoho National Park Canada
"SGU" -> 5, //Zion National Park
"STS" -> 5, //Sedona
"SVG" -> 5, //Salzburg
"BRW" -> 5, //Denali National Park USA
"CNY" -> 5, //Arches National Park USA
"ZUH" -> 5, //Zhuhai
"SAN" -> 5, //San Diego USA
"HDN" -> 5, //Hayden Steamboat Springs Colorado USA
"CLQ" -> 5, //Nevado de Colima National Park Mexico
"CCK" -> 5,
"XCH" -> 5,
"NLK" -> 5,
"SUV" -> 5,
"LDH" -> 5
 ),
    FINANCIAL_HUB -> Map[String, Int](
"SIN" -> 80, //Singapore
"JFK" -> 75, //New York
"HND" -> 75, //Tokyo
"LHR" -> 75, //London
"FRA" -> 65, //Frankfurt
"HKG" -> 60, //Hong Kong
"CDG" -> 60, //Paris
"MUC" -> 60, //Munich
"YYZ" -> 60, //Toronto
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
"ICN" -> 42, //Seoul
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
"ARN" -> 34, //Stockholm
"DOH" -> 34, //Doha
"CPH" -> 34, //Copenhagen
"HAM" -> 34, //Hamburg
"MEL" -> 34, //Melbourne
"KWI" -> 33, //Kuwait City
"TLV" -> 33, //Tel Aviv
"SCL" -> 33, //Santiago
"YUL" -> 32, //Montreal
"VIE" -> 32, //Vienna
"DME" -> 31, //Moscow
"SFO" -> 30, //San Francisco
"OSL" -> 30, //Oslo
"MEX" -> 30, //Mexico City
"LUX" -> 30, //Luxembourg
"ITM" -> 30, //Osaka
"KIX" -> 30, //Osaka
"BOM" -> 30, //Mumbai
"SVO" -> 29, //Moscow
"EDI" -> 29, //Edinburgh
"ATL" -> 28, //Atlanta
"IST" -> 28, //Istanbul
"PUS" -> 28, //Busan
"BOG" -> 26, //Bogota
"EZE" -> 26, //Buenos Aires
"CPT" -> 25, //Cape Town
"FCO" -> 25, //Rome
"MXP" -> 25, //Milan
"ORY" -> 25, //Paris
"PKX" -> 25, //Beijing
"YYC" -> 25, //Calgary
"BAH" -> 25, //Bahrain
"CLT" -> 24, //Charlotte
"GMP" -> 24, //Seoul
"SHA" -> 24, //Shanghai
"GIG" -> 22, //Rio de Janeiro
"RUH" -> 21, //Riyadh
"LIM" -> 21, //Lima
"NGO" -> 21, //Nagoya
"TAS" -> 20, //Tashkent
"IAD" -> 20, //Washington DC
"DEN" -> 20, //Denver
"DCA" -> 20, //Washington DC
"MDW" -> 20, //Chicago
"IAH" -> 20, //Houston
"BKK" -> 20, //Bangkok
"FUK" -> 20, //Fukuoka
"LIN" -> 20, //Milan
"BUD" -> 20, //Budapest
"LCY" -> 20, //London
"HAJ" -> 20, //Hanover
"WAW" -> 19, //Warsaw
"PRG" -> 19, //Prague
"MSP" -> 18, //Minneapolis
"AKL" -> 18, //Auckland
"LOS" -> 18, //Lagos
"NBO" -> 18, //Nairobi
"BLQ" -> 18, //Bologna
"DEL" -> 18, //New Delhi
"BLR" -> 18, //Bangalore
"MAN" -> 18, //Manchester
"BCN" -> 18, //Barcelona
"BSB" -> 18, //Brasilia
"JED" -> 17, //Jeddah
"OAK" -> 16, //San Francisco
"MNL" -> 16, //Manila
"HYD" -> 16, //Hyderabad
"TLL" -> 16, //Tallinn
"DUS" -> 16, //Dusseldorf
"CGN" -> 16, //Cologne
"MIA" -> 15, //Miami
"SEA" -> 15, //Seattle
"PHX" -> 15, //Phoenix
"SJC" -> 15, //San Francisco
"DAL" -> 15, //Dallas
"DTW" -> 15, //Detroit
"PTY" -> 15, //Panama City
"RTM" -> 15, //The Hague
"RMO" -> 15, //Chisinau
"HEL" -> 15, //Helsinki
"CGH" -> 15, //Sao Paulo
"SGN" -> 14, //Ho Chi Minh City
"PHL" -> 14, //Philadelphia
"KHH" -> 14, //Kaohsiung
"TRN" -> 14, //Turin
"IKA" -> 14, //Tehran
"AMD" -> 14, //GIFT City-Gujarat
"PER" -> 14, //Perth
"BNE" -> 14, //Brisbane
"ALA" -> 13, //Almaty
"TLS" -> 13, //Toulouse
"AUS" -> 12, //Austin
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
"BWI" -> 10, //Baltimore
"SAN" -> 10, //San Diego
"PDX" -> 10, //Portland
"TSA" -> 10, //Taipei
"DMK" -> 10, //Bangkok
"LED" -> 10, //St Petersburg
"RIX" -> 10, //Riga
"MAA" -> 10, //Chennai
"SDU" -> 10, //Rio de Janeiro
"AEP" -> 10, //Buenos Aires
"BGO" -> 9, //Bergen
"MLA" -> 9, //Malta
"ATH" -> 9, //Athens
"TFU" -> 9, //Chengdu
"BNA" -> 8, //Nashville
"BTS" -> 8, //Bratislava
"GOT" -> 8, //Gothenburg
"OTP" -> 8, //Bucharest
"KHI" -> 8, //Karachi
"CCU" -> 8, //Kolkata
"CKG" -> 8, //Jakarta
"NCL" -> 8, //Newcastle
"LYS" -> 8, //Grenoble
"BLL" -> 8, //Aarhus
"LCA" -> 8, //Nicosia
"NKG" -> 8, //Nanjing
"CTU" -> 8, //Chengdu
"BDA" -> 8, //Bermuda
"ADL" -> 8, //Adelaide
"PIT" -> 7, //Pittsburgh
"NQZ" -> 7, //Nur-Sultan
"DLC" -> 7, //Dalian
"GYD" -> 7, //Baku
"PNQ" -> 6, //Pune
"HGH" -> 6, //Hangzhou
"NAS" -> 6, //Nassau
"SOF" -> 6, //Sofia
"HAN" -> 5, //Hanoi
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
"MDE" -> 5, //Medellin
"CLO" -> 5, //Cali
"XIY" -> 5, //Xi'an
"YQB" -> 5, //Quebec City
"BEG" -> 5 //Belgrade
    ), 
    DOMESTIC_AIRPORT -> Map(
      "LGA" -> 0,
      "DCA" -> 0,
      "MDW" -> 0,
      "SNA" -> 0,
      "BUR" -> 0,
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
      "YZF" -> 0,
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
