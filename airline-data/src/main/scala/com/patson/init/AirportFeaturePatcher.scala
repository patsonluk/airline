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
*destinations with unusually more premium & intercontinental visitors relative to population
**/

//EU hubs+++ as they're relatively low pop
"LHR" -> 65, //London
"CDG" -> 60, //Paris
"AMS" -> 40, //Amsterdam
"FRA" -> 35, //Frankfurt
"FCO" -> 30, //Rome
"MXP" -> 25, //Milan
"ZRH" -> 25, //Zurich
"MAD" -> 25, //Madrid
"GVA" -> 20, //Geneva; low-pop
"BRU" -> 20, //Brussels
"DUB" -> 15, //Dublin
"CPH" -> 15, //Copenhagen
"ARN" -> 15, //Stockholm
"DME" -> 15, //Moscow
"SVO" -> 15, //Moscow
"ORY" -> 10, //Paris
"LGW" -> 10, //London
"ATH" -> 10, //Athens
"MUC" -> 10, //Munich
"BCN" -> 10, //Barcelona
"NCE" -> 10, //Nice
"OSL" -> 10, //Oslo
"LIS" -> 10, //Lisbon
"VIE" -> 5, //Vienna
"HEL" -> 5, //Helsinki
"WAW" -> 5, //Warsaw

//north-america
"JFK" -> 50, //New York
"SFO" -> 45, //San Francisco; low-pop
"YYZ" -> 30, //Toronto
"BOS" -> 30, //Boston; medium-pop
"LAX" -> 25, //Los Angeles
"MIA" -> 25, //Miami; medium-pop
"IAD" -> 25, //Washington DC; low-pop airport
"IAH" -> 20, //Houston
"SEA" -> 20, //Seattle
"MEX" -> 15, //Mexico City; very high-pop
"YVR" -> 15, //Vancouver
"LAS" -> 15, //Las Vegas
"HNL" -> 15, //Honolulu
"ORD" -> 10, //Chicago
"ATL" -> 10, //Atlanta; high-pop
"DFW" -> 10, //Dallas Fort Worth
"EWR" -> 10, //New York
"DTW" -> 5, //Detroit
"DEN" -> 5, //Denver
"CLT" -> 5, //Charlotte
"SLC" -> 5, //Salt Lake City
"PHX" -> 5, //Phoenix
"MSP" -> 5, //Minneapolis

//oceania
"SYD" -> 50, //Sydney
"MEL" -> 20, //Melbourne
"AKL" -> 10, //Auckland

//east-asia
"SIN" -> 65, //Singapore; medium pop
"NRT" -> 60, //Tokyo
"HKG" -> 45, //Hong Kong
"TPE" -> 35, //Taipei
"PVG" -> 30, //Shanghai
"ICN" -> 30, //Seoul
"HND" -> 30, //Tokyo; huge pop
"CAN" -> 20, //Guangzhou
"SZX" -> 15, //Shenzhen
"BKK" -> 15, //Bangkok
"KUL" -> 15, //Kuala Lumpur
"KIX" -> 15, //Osaka
"PEK" -> 10, //Beijing; huge pop
"CGK" -> 10, //Jakarta
"SGN" -> 10, //Ho Chi Minh City
"CTU" -> 5, //Chengdu
"KMG" -> 5, //Kunming
"MNL" -> 5, //Manila
"XIY" -> 5, //Xi'an

//india
"BOM" -> 10, //Mumbai
"DEL" -> 5, //New Delhi; 150m pop so ~same as 100 charm on 7m pop airport

//MENA
"DXB" -> 20, //Dubai
"JNB" -> 20, //Johannesburg
"JED" -> 15, //Jeddah
"IST" -> 15, //Istanbul
"AUH" -> 10, //Abu Dhabi
"DOH" -> 10, //Doha
"RUH" -> 5, //Riyadh
"CAI" -> 5, //Cairo
"ADD" -> 5, //Addis Ababa
"NBO" -> 5, //Nairobi
"TLV" -> 5, //Tel Aviv

//south-america
"GRU" -> 25, //Sao Paulo
"SCL" -> 15, //Santiago
"EZE" -> 10, //Buenos Aires
"BOG" -> 5, //Bogota
"LIM" -> 5, //Lima
"PTY" -> 5 //Panama
    ),
    VACATION_HUB -> Map[String, Int](
/**
*destinations with unusually more tourists relative to population
**/
"HNL" -> 100, //Honolulu
"BKK" -> 100, //Bangkok
"CDG" -> 65, //Paris
"MCO" -> 113, //Orlando
"MLE" -> 55, //Malé
"JTR" -> 46, //Santorini Island
"PEK" -> 150, //Beijing
"DPS" -> 144, //Denpasar-Bali Island
"NAN" -> 41, //Nadi
"VCE" -> 42, //Venice
"ATH" -> 46, //Athens
"DXB" -> 82, //Dubai
"MLA" -> 39, //Valletta
"PRG" -> 44, //Prague
"LPA" -> 57, //Gran Canaria Island
"FCO" -> 38, //Rome
"IST" -> 49, //Istanbul
"TFS" -> 50, //Tenerife Island
"PPT" -> 34, //Papeete
"HER" -> 44, //Heraklion
"CNS" -> 54, //Cairns
"PUJ" -> 129, //Punta Cana
"OKA" -> 92, //Naha
"CUN" -> 150, //Cancún
"JED" -> 32, //Jeddah
"GUM" -> 40, //Hagåtña, Guam International Airport
"KIX" -> 30, //Osaka
"LAS" -> 113, //Las Vegas
"MIA" -> 30, //Miami
"SIN" -> 73, //Singapore
"HKG" -> 110, //Hong Kong
"NRT" -> 28, //Tokyo / Narita
"VIE" -> 30, //Vienna
"BCN" -> 86, //Barcelona
"HAV" -> 68, //Havana
"MRS" -> 40, //Marseille
"MXP" -> 27, //Milan
"SJD" -> 75, //San José del Cabo
"GIG" -> 29, //Rio De Janeiro
"KOA" -> 25, //Kailua-Kona
"NAS" -> 37, //Nassau
"DBV" -> 44, //Dubrovnik
"KEF" -> 24, //Reykjavík
"BUD" -> 24, //Budapest
"NCE" -> 22, //Nice
"PSA" -> 44, //Pisa
"OSL" -> 21, //Oslo
"AMS" -> 22, //Amsterdam
"TLV" -> 5, //Tel Aviv
"CPH" -> 21, //Copenhagen
"CAI" -> 5, //Cairo
"SPX" -> 25, //Cairo
"LHR" -> 20, //London
"BOM" -> 49, //Mumbai
"LAX" -> 20, //Los Angeles
"SNA" -> 10, //Santa Ana
"YVR" -> 20, //Vancouver
"IAD" -> 25, //Washington, DC
"SJU" -> 43, //San Juan
"GRU" -> 70, //São Paulo
"BNE" -> 135, //Brisbane
"AKL" -> 54, //Auckland
"SFO" -> 20, //San Francisco
"OOL" -> 74, //Gold Coast
"CMB" -> 52, //Colombo
"MRU" -> 42, //Port Louis
"KIN" -> 37, //Kingston
"BWN" -> 31, //Bandar Seri Begawan
"MFM" -> 71, //Macau
"CTS" -> 133, //Chitose / Tomakomai
"HKT" -> 150, //Phuket
"PMI" -> 110, //Palma De Mallorca
"KUL" -> 119, //Kuala Lumpur
"LIS" -> 28, //Lisbon
"OGG" -> 58, //Kahului
"SYX" -> 70, //Sanya
"BAR" -> 35, //Qionghai
"XIY" -> 70, //Xi'an
"PVG" -> 10, //Shanghai
"PER" -> 92, //Perth
"FLN" -> 67, //Florianópolis
"NAP" -> 25, //Nápoli
"KRK" -> 31, //Kraków
"VRA" -> 43, //Varadero
"HEL" -> 27, //Helsinki
"LED" -> 25, //St. Petersburg
"CJU" -> 142, //Jeju City
"HGH" -> 42, //Hangzhou
"SJO" -> 27, //San Jose
"LIH" -> 16, //Lihue
"CEB" -> 88, //Lapu-Lapu City
"ITO" -> 15, //Hilo
"WAW" -> 15, //Warsaw
"KWL" -> 31, //Guilin City
"DRW" -> 18, //Darwin
"STX" -> 14, //Christiansted
"MBJ" -> 76, //Montego Bay
"LXA" -> 54, //Lhasa
"MYR" -> 24, //Myrtle Beach
"KBV" -> 135, //Krabi
"AUA" -> 35, //Oranjestad
"BGI" -> 16, //Bridgetown
"IBZ" -> 46, //Ibiza
"TAO" -> 40, //Qingdao
"NOU" -> 10, //Nouméa
"YZF" -> 10, //Yellowknife
"CUR" -> 25, //Willemstad
"DMK" -> 100, //Bangkok
"ORY" -> 32, //Paris
"SAW" -> 21, //Istanbul
"TFN" -> 25, //Tenerife Island
"CHQ" -> 14, //Heraklion
"FLL" -> 22, //Miami
"HND" -> 14, //Tokyo / Narita
"SDU" -> 13, //Rio De Janeiro
"UNA" -> 15, //Transamérica Resort, Comandatuba Island
"DCA" -> 25, //Washington, DC
"CGH" -> 35, //São Paulo
"SHJ" -> 12, //Dubai
"BGY" -> 10, //Milan
"BWI" -> 25, //Washington, DC
"EZE" -> 20, //Buenos Aires
"BRC" -> 25, //San Carlos de Bariloche
"IGR" -> 44, //Puerto Iguazu
"USH" -> 16, //Ushuahia
"FTE" -> 17, //El Calafate
"NQN" -> 32, //Neuquen
"LPB" -> 52, //La Paz / El Alto
"IGU" -> 40, //Foz Do IguaÃ§u
"NVT" -> 54, //Navegantes
"IOS" -> 17, //IlhÃ©us
"FEN" -> 10, //Fernando De Noronha
"BPS" -> 36, //Porto Seguro
"VIX" -> 39, //VitÃ³ria
"FOR" -> 113, //Fortaleza
"REC" -> 43, //Recife
"SSA" -> 23, //Salvador
"PUQ" -> 33, //Punta Arenas
"CJC" -> 44, //Calama
"PMC" -> 66, //Puerto Montt
"IPC" -> 10, //Isla De Pascua
"SCL" -> 65, //Santiago
"ADZ" -> 34, //San AndrÃ©s
"CTG" -> 68, //Cartagena, Colombia
"SMR" -> 30, //Santa Marta, Colombia
"BAQ" -> 12, //Barranquilla, Colombia
"GPS" -> 20, //Baltra
"LIR" -> 29, //Liberia
"CUZ" -> 75, //Cusco
"MVD" -> 29, //Montevideo
"PMV" -> 87, //Isla Margarita
"ANU" -> 17, //St. John's
"BON" -> 10, //Kralendijk, Bonaire
"CCC" -> 12, //Cayo Coco
"SDQ" -> 19, //Santo Domingo
"STI" -> 19, //Santiago
"PTP" -> 22, //Pointe-Ã -Pitre
"RTB" -> 17, //Roatan Island
"GCM" -> 12, //Georgetown
"UVF" -> 14, //Vieux Fort
"FDF" -> 10, //Fort-de-France
"SXM" -> 35, //Saint Martin
"STT" -> 52, //Charlotte Amalie, Harry S. Truman Airport
"YYC" -> 24, //Calgary
"YUL" -> 18, //Montreal
"YYT" -> 14, //St John
"YHZ" -> 12, //Halifax
"YQB" -> 12, //Quebec
"MEX" -> 30, //Mexico City
"CZM" -> 42, //Cozumel
"PVR" -> 92, //Puerto Vallarta
"LAP" -> 34, //La Paz
"CTM" -> 21, //Chetumal
"HUX" -> 29, //Huatulco
"TGZ" -> 43, //Tuxtla GutiÃ©rrez
"MID" -> 21, //Mérida
"ANC" -> 11, //Anchorage
"FAI" -> 8,  //Fairbanks
"JNU" -> 15, //Juneau
"KTN" -> 12, //Ketchikan
"BZN" -> 16, //Bozeman
"ASE" -> 15, //Aspen
"BUF" -> 35, //Buffalo
"RNO" -> 23, //Reno
"RSW" -> 32, //Fort Myers
"GCN" -> 20, //Grand Canyon
"JAC" -> 14, //Jackson
"EYW" -> 20, //Key West
"BNA" -> 51, //Nashville
"ACK" -> 21, //Nantucket
"HYA" -> 11, //Cape Cod
"MSY" -> 67, //New Orleans
"JFK" -> 30, //New York
"SLC" -> 53, //Salt Lake City
"MTJ" -> 9, //Montrose (Ski resort)
"TPA" -> 64, //Tampa
"SAI" -> 69, //Siem Reap
"PNH" -> 67, //Phnom Penh
"KOS" -> 56, //Sihanukville
"HAK" -> 90, //Haikou
"TRV" -> 36, //Thiruvananthapuram
"COK" -> 47, //Kochi
"CCJ" -> 43, //Calicut
"GOI" -> 75, //Vasco da Gama
"SXR" -> 67, //Srinagar
"JAI" -> 75, //Jaipur
"VNS" -> 64, //Varanasi
"AGX" -> 12, //Agatti
"IXZ" -> 9, //Port Blair
"BTH" -> 51, //Batam Island
"LOP" -> 36, //Mataram
"YIA" -> 96, //Yogyakarta
"NGO" -> 10, //Tokoname
"FUK" -> 10, //Fukuoka
"LPQ" -> 24, //Luang Phabang
"LGK" -> 90, //Langkawi
"BKI" -> 98, //Kota Kinabalu
"KTM" -> 39, //Kathmandu
"ICN" -> 7, //Seoul
"PPS" -> 26, //Puerto Princesa City
"MPH" -> 55, //Malay
"CNX" -> 150, //Chiang Mai
"USM" -> 70, //Na Thon (Ko Samui Island)
"UTP" -> 90, //Rayong
"DAD" -> 113, //Da Nang
"PQC" -> 98, //Phu Quoc Island
"CXR" -> 113, //Nha Trang
"GYD" -> 30, //Baku
"BAH" -> 76, //Manama
"TBS" -> 14, //Tbilisi
"ETM" -> 7, //Eilat
"MHD" -> 51, //Mashhad
"IKA" -> 34, //Tehran
"KIH" -> 34, //Kish Island
"AMM" -> 34, //Amman
"SLL" -> 25, //Salalah
"MED" -> 45, //Medina
"ASR" -> 51, //Kayseri
"GZP" -> 38, //GazipaÅŸa
"AYT" -> 150, //Antalya
"ADB" -> 10, //Izmir
"BJV" -> 52, //Bodrum
"SID" -> 14, //Espargos
"BVC" -> 18, //Rabil
"RAI" -> 10, //Praia
"RMF" -> 39, //Marsa Alam
"SSH" -> 57, //Sharm el-Sheikh
"LXR" -> 8,  //Luxor
"HRG" -> 74, //Hurghada
"BJL" -> 10, //Banjul
"NBO" -> 49, //Nairobi
"AGA" -> 23, //Agadir
"RAK" -> 134, //Marrakech
"RUN" -> 43, //St Denis
"EBB" -> 45, //Kampala
"SEZ" -> 14, //Mahe Island
"ZNZ" -> 34, //Zanzibar
"JRO" -> 54, //Arusha
"NBE" -> 19, //Enfidha
"DJE" -> 27, //Djerba
"MIR" -> 18, //Monastir
"CPT" -> 70, //Cape Town
"JNB" -> 40, //Johannesburg
"MQP" -> 32, //Mpumalanga
"LVI" -> 66, //Livingstone
"VFA" -> 66, //Victoria Falls
"MUB" -> 28,
"BOJ" -> 28, //Burgas
"VAR" -> 56, //Varna
"RVN" -> 14, //Rovaniemi
"LYS" -> 10, //Lyon
"AJA" -> 27, //Ajaccio/NapolÃ©on Bonaparte
"BIA" -> 24, //Bastia-Poretta
"CLY" -> 15, //Calvi-Sainte-Catherine
"LCA" -> 33, //Larnarca
"BER" -> 87, //Berlin
"MUC" -> 12, //Munich
"HAM" -> 38, //Hamburg
"ALC" -> 38, //Alicante
"FUE" -> 38, //Fuerteventura Island
"GRO" -> 39, //Girona
"ACE" -> 49, //Lanzarote Island
"AGP" -> 146, //MÃ¡laga
"REU" -> 28, //Reus
"VLC" -> 30, //Valencia
"MAD" -> 15, //Madrid
"FLR" -> 40, //Firenze
"BLQ" -> 11, //Bologna
"BRI" -> 20, //Bari
"CAG" -> 67, //Cagliari
"OLB" -> 47, //Olbia (SS)
"CTA" -> 44, //Catania
"PMO" -> 44, //Palermo
"ZTH" -> 32, //Zakynthos Island
"CFU" -> 48, //Kerkyra Island
"KGS" -> 43, //Kos Island
"RHO" -> 74, //Rodes Island
"EFL" -> 16, //Kefallinia Island
"JMK" -> 38, //Mykonos Island
"SPU" -> 20, //Split
"ZAD" -> 10, //Zemunik (Zadar)
"GDN" -> 36, //GdaÅ„sk
"FAO" -> 77, //Faro
"FNC" -> 29, //Funchal
"TIV" -> 30, //Tivat
"ARN" -> 10, //Stockholm
"LLA" -> 16, //LuleÃ¥
"OTP" -> 5, //Bucharest
"KZN" -> 44, //Kazan
"KRR" -> 66, //Krasnodar
"SVO" -> 16, //Moscow
"ZIA" -> 5, //Moscow
"IKT" -> 21, //Irkutsk
"AER" -> 72, //Sochi
"VOG" -> 11, //Volgograd
"EDI" -> 40, //Edinburgh
"SIP" -> 48, //Simferopol
"TSV" -> 33, //Townsville
"HTI" -> 21, //Hamilton Island Resort
"PPP" -> 14, //Whitsunday Coast Airport
"AYQ" -> 10, //Ayers Rock
"BME" -> 10, //Broome
"HBA" -> 47, //Hobart
"ASP" -> 12, //Alice Springs
"LIF" -> 10, //Lifou
"CHC" -> 137, //Christchurch
"LGK" -> 89, //Langkawi
"PEN" -> 43, //Penang
"SKD" -> 34, //Samarkand
"MAH" -> 27, //Menorca Island
"AEP" -> 69, //Buenos Aires
"LGA" -> 19, //New York
"SRQ" -> 10, //Sarasota/Bradenton
"THR" -> 34, //Tehran
"FSC" -> 10, //Figari Sud-Corse
"PFO" -> 10, //Paphos
"BDS" -> 10, //Brindisi
"DME" -> 10, //Moscow
"ECN" -> 12, //Nicosia
"VKO" -> 10, //Moscow
"ZRH" -> 20, //Zurich
"ZQN" -> 15, //Queenstown
"SGU" -> 5, //Zion National Park
"FCA" -> 5, //Glacier National Park
"STS" -> 5 //Sedona
   
 ),
    FINANCIAL_HUB -> Map[String, Int](
"LHR" -> 80, //London
"JFK" -> 80, //New York
"HND" -> 79, //Tokyo
"HKG" -> 70, //Hong Kong
"SIN" -> 71, //Singapore
"FRA" -> 58, //Frankfurt
"ICN" -> 50, //Seoul
"PVG" -> 50, //Shanghai
"DXB" -> 55, //Dubai
"ZRH" -> 59, //Zurich
"BOS" -> 57, //Boston
"SFO" -> 80, //San Francisco
"CDG" -> 53, //Paris
"DFW" -> 37, //Dallas Fort Worth
"ORD" -> 60, //Chicago
"DEN" -> 34, //Denver
"CAN" -> 61, //Guangzhou
"SZX" -> 55, //Shenzhen
"IST" -> 31, //Istanbul
"MEX" -> 30, //Mexico City
"ITM" -> 34, //Osaka
"CLT" -> 28, //Charlotte
"ATL" -> 58, //Atlanta
"TSN" -> 29, //Tianjin
"MUC" -> 65, //Munich
"SEA" -> 30, //Seattle
"BHX" -> 22, //Birmingham
"DEL" -> 33, //New Delhi
"PHX" -> 22, //Phoenix
"PUS" -> 50, //Busan
"MEL" -> 59, //Melbourne
"KHH" -> 22, //Kaohsiung
"IAH" -> 21, //Houston
"YYZ" -> 67, //Toronto
"FUK" -> 20, //Fukuoka
"MSP" -> 36, //Minneapolis
"CPT" -> 35, //Cape Town
"DMK" -> 10, //Bangkok
"CPH" -> 54, //Copenhagen
"DCA" -> 39, //Washington DC
"SVO" -> 19, //Moscow
"NGO" -> 18, //Nagoya
"MAN" -> 18, //Manchester
"HAM" -> 54, //Hamburg
"BWI" -> 21, //Baltimore
"DUS" -> 18, //Dusseldorf
"CGN" -> 18, //Cologne
"OTP" -> 18, //Bucharest
"BSB" -> 18, //Brasilia
"YUL" -> 56, //Montreal
"PHL" -> 25, //Philadelphia
"JED" -> 17, //Jeddah
"MAA" -> 17, //Chennai
"CKG" -> 27, //Jakarta
"ARN" -> 57, //Stockholm
"BER" -> 67, //Berlin
"SCL" -> 33, //Santiago
"FLL" -> 15, //Fort Lauderdale
"BOG" -> 26, //Bogota
"RUH" -> 26, //Riyadh
"CTU" -> 54, //Chengdu
"WUH" -> 14, //Wuhan
"NKG" -> 29, //Nanjing
"CMN" -> 27, //Casablanca
"DUB" -> 44, //Dublin
"LOS" -> 28, //Lagos
"MNL" -> 31, //Manila
"LIM" -> 12, //Lima
"ALG" -> 12, //Algiers
"NBO" -> 24, //Nairobi
"SGN" -> 24, //Ho Chi Minh City
"SYD" -> 33, //Sydney
"AMS" -> 40, //Amsterdam
"GVA" -> 54, //Geneva
"LUX" -> 52, //Luxembourg
"EDI" -> 56, //Edinburgh
"AUH" -> 35, //Abu Dhabi
"OSL" -> 52, //Oslo
"TAO" -> 49, //Qingdao
"STR" -> 56, //Stuttgart
"MAD" -> 36, //Madrid
"YVR" -> 35, //Vancouver
"YYC" -> 50, //Calgary
"GLA" -> 50, //Glasgow
"HEL" -> 53, //Helsinki
"WLG" -> 46, //Wellington
"BRU" -> 50, //Brussels
"MXP" -> 19, //Milan
"VIE" -> 49, //Vienna
"FCO" -> 50, //Rome
"LIS" -> 45, //Lisbon
"TPE" -> 42, //Taipei
"KUL" -> 44, //Kuala Lumpur
"DOH" -> 29, //Doha
"SAN" -> 46, //San Diego
"JER" -> 34, //Jersey
"PRG" -> 37, //Prague
"WAW" -> 37, //Warsaw
"MLA" -> 34, //Malta
"JNB" -> 33, //Johannesburg
"NQZ" -> 33, //Nursultan Nazarbayev Nur-Sultan
"BOM" -> 32, //Mumbai
"GCI" -> 32, //Guernsey
"BDA" -> 32, //Bermuda
"AMD" -> 31, //GIFT City-Gujarat
"LCA" -> 31, //Nicosia
"IOM" -> 31, //Castletown
"DLC" -> 30, //Dalian
"BAH" -> 30, //Bahrain
"BTS" -> 29, //Bratislava
"HGH" -> 47, //Hangzhou
"NCE" -> 29, //Monaco
"GRU" -> 18, //Sao Paulo
"ALA" -> 28, //Almaty
"GIG" -> 18, //Rio de Janeiro
"EIS" -> 28, //Road Town, VI
"MRU" -> 27, //Port Louis
"TLL" -> 31, //Tallinn
"KEF" -> 32, //Reykjavik
"ATH" -> 27, //Athens
"BUD" -> 28, //Budapest
"KGL" -> 26, //Kigali
"GIB" -> 26, //Gibraltar
"SOF" -> 25, //Sofia
"VNO" -> 23, //Vilnius
"RIX" -> 23, //Riga
"KWI" -> 23, //Kuwait City
"NAS" -> 22, //Nassau
"PTY" -> 22, //Panama City
"IKA" -> 22, //Tehran
"POS" -> 21, //Port of Spain
"LED" -> 21, //St Petersburg
"EZE" -> 21, //Buenos Aires
"GYD" -> 19, //Baku
"BGI" -> 18, //Bridgetown
"XIY" -> 14, //Xi'an
"MIA" -> 54, //Miami
"KHI" -> 24, //Karachi
"PLS" -> 15, //Providenciales
"RMO" -> 16, //Chisinau
"KUN" -> 12, //Kaunas
"TAS" -> 20, //Tashkent
"LAX" -> 30, //Los Angeles
"PEK" -> 63, //Beijing
"AUS" -> 20, //Austin
"PIT" -> 18, //Pittsburgh
"DTW" -> 20, //Detroit
"CMH" -> 11, //Columbus
"BNA" -> 19, //Nashville
"LAS" -> 18, //Las Vegas
"LYS" -> 18, //Grenoble
"BCN" -> 18, //Barcelona
"PDX" -> 17, //Portland
"TLS" -> 17, //Toulouse
"SMF" -> 14, //Sacramento
"HAJ" -> 24, //Hanover
"DTM" -> 16, //Dortmund
"BRS" -> 16, //Bristol
"BNE" -> 16, //Brisbane
"LEJ" -> 15, //Leipzig
"NCL" -> 15, //Newcastle
"ADL" -> 15, //Adelaide
"BOH" -> 15, //Southampton
"RTM" -> 15, //The Hague
"BLQ" -> 15, //Bologna
"AKL" -> 14, //Auckland
"BLL" -> 14, //Aarhus
"CBR" -> 14, //Canberra
"GOT" -> 39, //Gothenburg
"VCE" -> 14, //Padua
"TRN" -> 13, //Turin
"YQB" -> 12, //Quebec City
"LGW" -> 32, //London
"EWR" -> 33, //New York
"GMP" -> 24, //Seoul
"SHA" -> 34, //Shanghai
"OAK" -> 18, //San Francisco
"ORY" -> 25, //Paris
"MDW" -> 15, //Chicago
"KIX" -> 20, //Osaka
"IAD" -> 34, //Washington DC
"JNU" -> 5, //Juneau
"BKK" -> 20, //Bangkok
"DME" -> 19, //Moscow
"CGH" -> 10, //Sao Paulo
"SDU" -> 10, //Rio de Janeiro
"LGA" -> 19, //New York
"SJC" -> 20, //San Francisco
"LCY" -> 20, //London
"LIN" -> 20, //Milan
"AEP" -> 10, //Buenos Aires
"TSA" -> 10, //Taipei
"SVG" -> 5,  //Salzburg
"INN" -> 10  //Innsbruck
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
      //china
      "TFU" -> 0,
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
      "GIG", //Brazil
      "GRU",
      "NRT", //Japan
      "KIX",
      "SVO", //Russia
      "LED",
      "FCO", //Italy
      "MXP",
      "FRA", //Germany
      "MUC",
      "SYD", //Australia
      "MEL",
      "YVR", //Canada
      "YYZ"))
    list.toList
  }
}
