package com.patson.init

import com.patson.model._
import com.patson.data.AirportSource

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object AirportFeaturePatcher extends App {

  import AirportFeatureType._

  lazy val featureList = Map(
    INTERNATIONAL_HUB -> Map[String, Int](
      "JFK" -> 45, //New York
"LHR" -> 60, //London
"HKG" -> 50, //Hong Kong
"SIN" -> 50, //Singapore
"NRT" -> 30, //Tokyo
"CDG" -> 55, //Paris
"SYD" -> 30, //Sydney
"AUH" -> 20, //Abu Dhabi
"PVG" -> 55, //Shanghai
"SFO" -> 40, //San Francisco
"ZRH" -> 20, //Zurich
"FRA" -> 50, //Frankfurt
"LAX" -> 65, //Los Angeles
"TPE" -> 30, //Taipei
"MXP" -> 15, //Milan
"YYZ" -> 35, //Toronto
"FCO" -> 30, //Rome
"ICN" -> 50, //Seoul
"PEK" -> 75, //Beijing
"EZE" -> 10, //Buenos Aires
"MAD" -> 45, //Madrid
"AMS" -> 50, //Amsterdam
"KUL" -> 45, //Kuala Lumpur
"JNB" -> 20, //Johannesburg
"GRU" -> 25, //Sao Paulo
"IAD" -> 15, //Washington DC
"MIA" -> 30, //Miami
"YVR" -> 15, //Vancouver
"BKK" -> 45, //Bangkok
"BOS" -> 30, //Boston
"CGK" -> 35, //Jakarta
"KIX" -> 15, //Osaka
"ARN" -> 10, //Stockholm
"MEX" -> 35, //Mexico City
"BRU" -> 10, //Brussels
"DOH" -> 25, //Doha
"WAW" -> 10, //Warsaw
"BOM" -> 30, //Mumbai
"ATL" -> 80, //Atlanta
"DXB" -> 65, //Dubai
"HND" -> 60, //Tokyo
"ORD" -> 60, //Chicago
"DFW" -> 55, //Dallas Fort Worth
"CAN" -> 30, //Guangzhou
"DEN" -> 45, //Denver
"DEL" -> 45, //New Delhi
"IST" -> 50, //Istanbul
"CTU" -> 25, //Chengdu
"SZX" -> 25, //Shenzhen
"BCN" -> 25, //Barcelona
"SEA" -> 35, //Seattle
"LAS" -> 15, //Las Vegas
"CLT" -> 35, //Charlotte
"SVO" -> 30, //Moscow
"KMG" -> 15, //Kunming
"MUC" -> 30, //Munich
"MNL" -> 30, //Manila
"XIY" -> 15, //Xi'an
"LGW" -> 20, //London
"EWR" -> 30, //New York
"PHX" -> 25, //Phoenix
"SHA" -> 15, //Shanghai
"IAH" -> 30, //Houston
"DUB" -> 25, //Dublin
"ORY" -> 20, //Paris
"VIE" -> 15, //Vienna
"LIS" -> 20, //Lisbon
"CPH" -> 15, //Copenhagen
"OSL" -> 15, //Oslo
"DME" -> 15, //Moscow
"ATH" -> 10, //Athens
"HEL" -> 15, //Helsinki
"MSP" -> 25, //Minneapolis
"DTW" -> 20, //Detroit
"PHL" -> 15, //Philadelphia
"SLC" -> 10, //Salt Lake City
"HNL" -> 10, //Honolulu
"RUH" -> 15, //Riyadh
"CAI" -> 10, //Cairo
"CMN" -> 10, //Casablanca
"MEL" -> 20, //Melbourne
"BOG" -> 20, //Bogota
"LIM" -> 15, //Lima
"SCL" -> 15, //Santiago
"AKL" -> 10, //Auckland
"SGN" -> 25, //Ho Chi Minh City
"PTY" -> 15, //Panama
"ADD" -> 10, //Addis Ababa
"NBO" -> 10, //Nairobi
"JED" -> 15, //Jeddah
"KEF" -> 15 //Reykjavik

    
    ),
    VACATION_HUB -> Map[String, Int](
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
"HER" -> 38, //Heraklion
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
"HKG" -> 150, //Hong Kong
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
"TLV" -> 25, //Tel Aviv
"CPH" -> 21, //Copenhagen
"CAI" -> 40, //Cairo
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
"PMI" -> 130, //Palma De Mallorca
"KUL" -> 139, //Kuala Lumpur
"LIS" -> 28, //Lisbon
"OGG" -> 18, //Kahului
"SYX" -> 150, //Sanya
"XIY" -> 150, //Xi'an
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
"KWL" -> 150, //Guilin City
"DRW" -> 18, //Darwin
"STX" -> 14, //Christiansted
"MBJ" -> 76, //Montego Bay
"LXA" -> 139, //Lhasa
"MYR" -> 24, //Myrtle Beach
"KBV" -> 135, //Krabi
"AUA" -> 29, //Oranjestad
"BGI" -> 16, //Bridgetown
"IBZ" -> 46, //Ibiza
"TAO" -> 150, //Qingdao
"NOU" -> 10, //Nouméa
"YZF" -> 10, //Yellowknife
"CUR" -> 25, //Willemstad
"DMK" -> 100, //Bangkok
"ORY" -> 32, //Paris
"SAW" -> 21, //Istanbul
"TFN" -> 25, //Tenerife Island
"CHQ" -> 14, //Heraklion
"FLL" -> 22, //Miami
"HND" -> 28, //Tokyo / Narita
"SDU" -> 13, //Rio De Janeiro
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
"CTG" -> 68, //Cartagena
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
"YYC" -> 54, //Calgary
"CZM" -> 35, //Cozumel
"PVR" -> 92, //Puerto Vallarta
"LAP" -> 34, //La Paz
"CTM" -> 21, //Chetumal
"HUX" -> 29, //Huatulco
"TGZ" -> 43, //Tuxtla GutiÃ©rrez
"ANC" -> 11, //Anchorage
"BZN" -> 16, //Bozeman
"ASE" -> 10, //Aspen
"BUF" -> 35, //Buffalo
"RNO" -> 23, //Reno
"RSW" -> 32, //Fort Myers
"GCN" -> 10, //Grand Canyon
"JAC" -> 10, //Jackson
"EYW" -> 10, //Key West
"BNA" -> 51, //Nashville
"ACK" -> 10, //Nantucket
"MSY" -> 67, //New Orleans
"JFK" -> 30, //New York
"SLC" -> 53, //Salt Lake City
"TPA" -> 64, //Tampa
"REP" -> 69, //Siem Reap
"PNH" -> 67, //Phnom Penh
"KOS" -> 56, //Sihanukville
"HAK" -> 150, //Haikou
"TRV" -> 36, //Thiruvananthapuram
"COK" -> 47, //Kochi
"CCJ" -> 43, //Calicut
"GOI" -> 75, //Vasco da Gama
"SXR" -> 67, //Srinagar
"JAI" -> 75, //Jaipur
"VNS" -> 64, //Varanasi
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
"ICN" -> 0, //Seoul
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
"ETM" -> 33, //Eilat
"MHD" -> 113, //Mashhad
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
"CPT" -> 150, //Cape Town
"JNB" -> 90, //Johannesburg
"MQP" -> 32, //Mpumalanga
"LVI" -> 66, //Livingstone
"VFA" -> 66, //Victoria Falls
"BOJ" -> 28, //Burgas
"VAR" -> 56, //Varna
"RVN" -> 14, //Rovaniemi
"LYS" -> 10, //Lyon
"AJA" -> 27, //Ajaccio/NapolÃ©on Bonaparte
"LCA" -> 33, //Larnarca
"BER" -> 87, //Berlin
"MUC" -> 12, //Munich
"HAM" -> 38, //Hamburg
"ALC" -> 38, //Alicante
"FUE" -> 32, //Fuerteventura Island
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
"JMK" -> 33, //Mykonos Island
"SPU" -> 20, //Split
"ZAD" -> 10, //Zemunik (Zadar)
"GDN" -> 36, //GdaÅ„sk
"FAO" -> 77, //Faro
"FNC" -> 29, //Funchal
"TIV" -> 30, //Tivat
"ARN" -> 10, //Stockholm
"LLA" -> 16, //LuleÃ¥
"OTP" -> 0, //Bucharest
"KZN" -> 44, //Kazan
"KRR" -> 66, //Krasnodar
"SVO" -> 16, //Moscow
"ZIA" -> 0, //Moscow
"IKT" -> 33, //Irkutsk
"AER" -> 72, //Sochi
"VOG" -> 11, //Volgograd
"EDI" -> 40, //Edinburgh
"SIP" -> 48, //Simferopol
"TSV" -> 33, //Townsville
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
"ZRH" -> 20 //Zurich   
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
"TLV" -> 52, //Tel Aviv
"TPE" -> 42, //Taipei
"KUL" -> 44, //Kuala Lumpur
"DOH" -> 29, //Doha
"SAN" -> 46, //San Diego
"JER" -> 34, //Jersey
"PRG" -> 37, //Prague
"WAW" -> 37, //Warsaw
"MLA" -> 34, //Malta
"JNB" -> 33, //Johannesburg
"TSE" -> 33, //Nur-Sultan
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
"EIS" -> 28, //Road Town
"MRU" -> 27, //Port Louis
"TLL" -> 31, //Tallinn
"KEF" -> 27, //Reykjavik
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
"KIV" -> 16, //Chisinau
"KUN" -> 17, //Kaunas
"TAS" -> 20, //Tashkent
"LAX" -> 30, //Los Angeles
"PEK" -> 63, //Beijing
"AUS" -> 25, //Austin
"PIT" -> 24, //Pittsburgh
"DTW" -> 20, //Detroit
"CMH" -> 20, //Columbus
"BNA" -> 19, //Nashville
"LAS" -> 18, //Las Vegas
"LYS" -> 18, //Grenoble
"BCN" -> 18, //Barcelona
"PDX" -> 17, //Portland
"TLS" -> 17, //Toulouse
"SMF" -> 17, //Sacramento
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
"MDW" -> 10, //Chicago
"KIX" -> 20, //Osaka
"IAD" -> 34, //Washington DC
"BKK" -> 20, //Bangkok
"DME" -> 19, //Moscow
"CGH" -> 10, //Sao Paulo
"SDU" -> 10, //Rio de Janeiro
"LGA" -> 19, //New York
"SJC" -> 20, //San Francisco
"LCY" -> 10, //London
"LIN" -> 20, //Milan
"AEP" -> 10, //Buenos Aires
"TSA" -> 10 //Taipei
    ), 
    DOMESTIC_AIRPORT -> Map[String, Int]()
  ) + (GATEWAY_AIRPORT -> getGatewayAirports().map(iata => (iata, 0)).toMap)

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

  def getGatewayAirports() : List[String] = {
    //The most powerful airport of every country
    val airportsByCountry = AirportSource.loadAllAirports().groupBy(_.countryCode).filter(_._2.length > 0)
    val topAirportByCountry = airportsByCountry.view.mapValues(_.sortBy(_.power).last)

    val baseList = topAirportByCountry.values.map(_.iata).toList

    val list: mutable.ListBuffer[String] = collection.mutable.ListBuffer(baseList:_*)

    list -= "HND" //not haneda, instead it should be NRT

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
