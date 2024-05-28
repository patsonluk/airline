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
"CUZ" -> 85, //Cusco
"KBV" -> 84, //Krabi
"DPS" -> 84, //Denpasar-Bali Island
"PQC" -> 82, //Phu Quoc Island
"IST" -> 80, //Istanbul
"CDG" -> 80, //Paris
"LGK" -> 76, //Langkawi
"AYT" -> 75, //Antalya
"LHR" -> 75, //London
"BKK" -> 66, //Bangkok
"DXB" -> 65, //Dubai
"HKG" -> 63, //Hong Kong
"PNH" -> 62, //Phnom Penh
"CUN" -> 58, //Cancún
"JED" -> 55, //Jeddah
"MLE" -> 55, //Malé Maldives
"JFK" -> 54, //New York
"NRT" -> 53, //Tokyo / Narita
"RAK" -> 51, //Marrakech
"CMB" -> 48, //Colombo
"FCO" -> 46, //Rome
"DMK" -> 45, //Bangkok
"HAV" -> 45, //Havana
"KUL" -> 43, //Kuala Lumpur
"CEB" -> 42, //Lapu-Lapu City
"SIN" -> 42, //Singapore
"VCE" -> 42, //Venice
"MIA" -> 42, //Miami
"AMS" -> 41, //Amsterdam
"LAX" -> 41, //Los Angeles
"MBJ" -> 40, //Montego Bay
"DAD" -> 40, //Da Nang
"KIX" -> 40, //Osaka
"RUN" -> 40, //St Denis
"CAI" -> 40, //Cairo Egypt
"GRU" -> 39, //São Paulo
"VIE" -> 39, //Vienna
"ATH" -> 39, //Athens
"BCN" -> 38, //Barcelona
"EWR" -> 38, //New York City USA
"LGW" -> 38, //London United Kingdom
"HND" -> 38, //Tokyo / Haneda
"BKI" -> 37, //Kota Kinabalu
"PUJ" -> 36, //Punta Cana
"CPT" -> 35, //Cape Town
"SPX" -> 35, //Cairo
"GIG" -> 35, //Rio De Janeiro
"HRG" -> 34, //Hurghada
"NCE" -> 34, //Nice
"YYZ" -> 34, //Toronto Canada
"ICN" -> 34, //Seoul
"BER" -> 33, //Berlin
"UTP" -> 32, //Rayong
"LOP" -> 32, //Mataram
"HNL" -> 31, //Honolulu
"SAI" -> 31, //Siem Reap
"AGA" -> 31, //Agadir
"SYD" -> 30, //Sydney Australia
"AGP" -> 30, //MÃ¡laga
"AEP" -> 30, //Buenos Aires
"USM" -> 30, //Na Thon (Ko Samui Island)
"ARN" -> 30, //Stockholm
"PEK" -> 29, //Beijing
"MXP" -> 29, //Milan
"DUB" -> 29, //Dublin Ireland
"MUC" -> 29, //Munich
"MEX" -> 29, //Mexico City
"MCO" -> 28, //Orlando
"MPH" -> 28, //Malay
"LIS" -> 28, //Lisbon
"PEN" -> 28, //Penang
"DJE" -> 28, //Djerba
"PRG" -> 28, //Prague
"ZNZ" -> 28, //Zanzibar
"PPT" -> 28, //Papeete
"AUA" -> 28, //Oranjestad
"SSH" -> 27, //Sharm el-Sheikh
"BOM" -> 27, //Mumbai
"LPB" -> 27, //La Paz / El Alto
"VRA" -> 27, //Varadero
"SCL" -> 26, //Santiago
"TLV" -> 26, //Tel Aviv
"BUD" -> 26, //Budapest
"KEF" -> 26, //Reykjavík
"BAH" -> 25, //Manama
"GOI" -> 25, //Vasco da Gama
"YVR" -> 25, //Vancouver
"ZQN" -> 25, //Queenstown
"CMN" -> 25, //Casablanca
"RHO" -> 24, //Rodes Island
"LAP" -> 24, //La Paz
"NOU" -> 24, //Nouméa
"PTP" -> 24, //Pointe-Ã -Pitre
"PMO" -> 23, //Palermo
"GUM" -> 23, //Hagåtña Guam International Airport
"KTM" -> 23, //Kathmandu
"CPH" -> 23, //Copenhagen
"ORY" -> 22, //Paris
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
"POA" -> 20, //Porto Alegre
"HER" -> 20, //Heraklion
"LPQ" -> 20, //Luang Phabang
"OSL" -> 20, //Oslo
"LIN" -> 20, //Milan Italian Alps
"CUR" -> 20, //Willemstad
"NAN" -> 20, //Nadi
"GAN" -> 20, //Maldives
"CAG" -> 20, //Cagliari
"FLR" -> 20, //Firenze
"FNC" -> 20, //Funchal
"MEL" -> 19, //Melbourne
"LAS" -> 19, //Las Vegas
"IBZ" -> 19, //Ibiza
"SJO" -> 19, //San Jose
"ZRH" -> 19, //Zurich
"EBB" -> 19, //Kampala
"SVO" -> 19, //Moscow
"CTG" -> 18, //Cartagena
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
"SGN" -> 18,
"CNS" -> 18, //Cairns
"LVI" -> 18, //Livingstone
"SJU" -> 18, //San Juan
"OGG" -> 17, //Kahului
"ASR" -> 17, //Kayseri
"STT" -> 17, //Charlotte Amalie
"CFU" -> 17, //Kerkyra Island
"LCA" -> 17, //Larnarca
"NBE" -> 17, //Enfidha
"RTB" -> 17, //Roatan Island
"ATL" -> 17,
"GPS" -> 17, //Baltra Galapagos
"MUB" -> 16, //Maun
"FDF" -> 16, //Fort-de-France
"WAW" -> 16, //Warsaw
"BSL" -> 16, //Mulhouse French/Swiss Alps
"BOG" -> 16,
"TIA" -> 16, //Triana
"DOH" -> 16,
"VFA" -> 16, //Victoria Falls
"BJV" -> 15, //Bodrum
"MAD" -> 15, //Madrid
"IKA" -> 15, //Tehran
"JMK" -> 15, //Mykonos Island
"ADB" -> 15, //Izmir
"BOD" -> 15, //prehistoric caves France
"FLG" -> 15, //Flagstaff Grand Canyon
"JNB" -> 15, //Johannesburg
"SJD" -> 14, //San José del Cabo
"PVR" -> 14, //Puerto Vallarta
"PER" -> 14, //Perth
"JRO" -> 14, //Arusha
"NAS" -> 14, //Nassau
"KRK" -> 14, //Kraków
"AMM" -> 14, //Amman
"SID" -> 14, //Espargos
"YZF" -> 14, //Yellowknife
"DME" -> 14, //Moscow
"MED" -> 13, //Medina
"LED" -> 13, //St. Petersburg
"KOS" -> 12, //Sihanukville
"BTH" -> 12, //Batam Island
"THR" -> 12, //Tehran
"PLZ" -> 12, //Addo Elephant National Park South Africa
"XIY" -> 12, //Xi'an
"TBS" -> 12, //Tbilisi
"RAI" -> 12, //Praia
"LBJ" -> 12, //Komodo National Park Indonesia
"VTE" -> 12, //Luang Prabang Laos
"SLC" -> 12, //Salt Lake City
"PTY" -> 12,
"JNU" -> 12, //Juneau
"JAI" -> 11, //Jaipur
"LIR" -> 11, //Liberia Costa Rica
"ANC" -> 11, //Anchorage
"TER" -> 11, //Azores
"SAW" -> 10, //Istanbul
"FAO" -> 10, //Faro
"DEL" -> 10,
"CZM" -> 10, //Cozumel
"IGU" -> 10, //Foz Do IguaÃ§u
"CJC" -> 10, //Calama
"VKO" -> 10, //Moscow
"CHQ" -> 10, //Heraklion
"CIA" -> 10, //Ostia Antica Italy
"GYD" -> 10, //Baku
"YUL" -> 10, //Montreal
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
"REC" -> 9, //Recife
"TGZ" -> 9, //Tuxtla GutiÃ©rrez
"NBO" -> 9, //Nairobi
"GND" -> 9,
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
"GDT" -> 8, //Cockburn Town
"CYO" -> 8, //Cayo Largo del Sur Cuba
"SMA" -> 8, //Azores
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
"DBV" -> 5, //Dubrovnik
"JTR" -> 5, //Santorini Island
"KIN" -> 5, //Kingston
"EDI" -> 5, //Edinburgh
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
"MHH" -> 5, //Marsh Harbour Bahammas
"AEY" -> 5, //Thingvellir National Park Iceland
"BOB" -> 5, //Bora Bora French Polynesia
"MRE" -> 5, //Maasai Mara National Reserve Kenya
"SEU" -> 5,
"MFU" -> 5,
"YXY" -> 5, //Whitehorse
"GHB" -> 5, //Governor's Harbour Bahamas
"GGT" -> 5, //Bahamas
"CYB" -> 5 //West End
    ),
    VACATION_HUB -> Map[String, Int](
"CJU" -> 190, //Jeju City
"CTS" -> 130, //Chitose / Tomakomai
"MEL" -> 94, //Melbourne
"PMI" -> 90, //Palma De Mallorca
"MCO" -> 90, //Orlando
"YIA" -> 86, //Yogyakarta
"SYD" -> 84, //Sydney Australia
"CUN" -> 80, //Cancún
"JED" -> 80, //Jeddah
"AGP" -> 80, //MÃ¡laga
"AER" -> 75, //Sochi
"SYX" -> 75, //Sanya
"HAK" -> 75, //Haikou
"HNL" -> 75, //Honolulu
"LAS" -> 75, //Las Vegas
"OKA" -> 70, //Naha
"LPA" -> 70, //Gran Canaria Island
"TFS" -> 70, //Tenerife Island
"CTG" -> 68, //Cartagena
"FLN" -> 67, //Florianópolis
"SXR" -> 67, //Srinagar
"VNS" -> 66, //Varanasi
"PUJ" -> 65, //Punta Cana
"RHO" -> 60, //Rodes Island
"SJD" -> 60, //San José del Cabo
"MFM" -> 60, //Macau
"KRR" -> 60, //Krasnodar
"PKX" -> 60, //Beijing China
"PMC" -> 60, //Puerto Montt
"PMV" -> 60, //Isla Margarita
"PVR" -> 59, //Puerto Vallarta
"OGG" -> 58, //Kahului
"BNA" -> 58, //Nashville
"OOL" -> 57, //Gold Coast
"VAR" -> 56, //Varna
"COK" -> 55, //Kochi
"SAW" -> 52, //Istanbul
"BAH" -> 51, //Manama
"MHD" -> 51, //Mashhad
"AYT" -> 50, //Antalya
"BCN" -> 50, //Barcelona
"BKI" -> 50, //Kota Kinabalu
"POA" -> 50, //Porto Alegre
"SJU" -> 50, //San Juan
"FAO" -> 50, //Faro
"BGO" -> 50, //Bergen
"GRU" -> 49, //São Paulo
"BNE" -> 49, //Brisbane
"CGH" -> 49, //São Paulo
"KOS" -> 48, //Sihanukville
"MLA" -> 48, //Valletta
"MSY" -> 47, //New Orleans
"OLB" -> 47, //Olbia (SS)
"DMK" -> 45, //Bangkok
"KUL" -> 45, //Kuala Lumpur
"CAG" -> 45, //Cagliari
"TRD" -> 45, //Trondheim
"HBA" -> 45, //Hobart
"BAR" -> 45, //Qionghai
"GIG" -> 44, //Rio De Janeiro
"GOI" -> 44, //Vasco da Gama
"KZN" -> 44, //Kazan
"RMF" -> 44, //Marsa Alam
"AEP" -> 42, //Buenos Aires
"GRO" -> 41, //Girona
"HRG" -> 40, //Hurghada
"UTP" -> 40, //Rayong
"USM" -> 40, //Na Thon (Ko Samui Island)
"SCL" -> 40, //Santiago
"CNS" -> 40, //Cairns
"BJV" -> 40, //Bodrum
"DEL" -> 40,
"DBV" -> 40, //Dubrovnik
"ITM" -> 40, //Osaka Japan
"LGA" -> 40, //New York
"RAK" -> 39, //Marrakech
"ASR" -> 39, //Kayseri
"BTH" -> 39, //Batam Island
"REC" -> 39, //Recife
"VIX" -> 39, //VitÃ³ria
"SHA" -> 39, //Shanghai China
"POP" -> 39, //Puerto Plata Dominican Republic
"MAD" -> 38, //Madrid
"CTA" -> 38, //Catania
"JTR" -> 38, //Santorini Island
"MRS" -> 38, //Marseille
"FUE" -> 38, //Fuerteventura Island
"GZP" -> 38, //GazipaÅŸa
"MBJ" -> 36, //Montego Bay
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
"CCJ" -> 35, //Calicut
"KGS" -> 35, //Kos Island
"IGR" -> 35, //Puerto Iguazu
"ALC" -> 35, //Alicante
"KWL" -> 35, //Guilin City
"LYS" -> 35, //Lyon
"HAN" -> 35, //Hanoi
"CZM" -> 34, //Cozumel
"HGH" -> 34, //Hangzhou
"ACE" -> 34, //Lanzarote Island
"GDN" -> 34, //GdaÅ„sk
"KIH" -> 34, //Kish Island
"NQN" -> 34, //Neuquen
"TAO" -> 34, //Qingdao
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
"LIS" -> 30, //Lisbon
"SSH" -> 30, //Sharm el-Sheikh
"PSA" -> 30, //Pisa
"CFU" -> 30, //Kerkyra Island
"IKA" -> 30, //Tehran
"JRO" -> 30, //Arusha
"THR" -> 30, //Tehran
"CJC" -> 30, //Calama
"VKO" -> 30, //Moscow
"TGZ" -> 30, //Tuxtla GutiÃ©rrez
"SKD" -> 30, //Samarkand
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
"BOS" -> 28,
"REU" -> 27, //Reus
"SLL" -> 27, //Salalah
"FLL" -> 26, //Miami
"MYR" -> 26, //Myrtle Beach
"SDU" -> 26, //Rio De Janeiro
"PXO" -> 26, //Peneda-Gerês National Park Portugal
"ORY" -> 25, //Paris
"IBZ" -> 25, //Ibiza
"VFA" -> 25, //Victoria Falls
"JMK" -> 25, //Mykonos Island
"EDI" -> 25, //Edinburgh
"BRC" -> 25, //San Carlos de Bariloche
"SPU" -> 25, //Split
"HIJ" -> 25, //Hiroshima
"KMQ" -> 25, //Kumamoto
"BWI" -> 25, //Washington
"NAS" -> 24, //Nassau
"CHQ" -> 24, //Heraklion
"CIA" -> 24, //Ostia Antica Italy
"BWN" -> 24, //Bandar Seri Begawan
"HAM" -> 24, //Hamburg
"AJA" -> 24, //Ajaccio/NapolÃ©on Bonaparte
"BIA" -> 24, //Bastia-Poretta
"NVT" -> 24, //Navegantes
"PPS" -> 24, //Puerto Princesa City
"STI" -> 24, //Santiago
"TPA" -> 24, //Tampa
"YYC" -> 24, //Calgary
"SDQ" -> 24, //Santo Domingo
"JAI" -> 22, //Jaipur
"SMR" -> 22, //Santa Marta
"IKT" -> 22, //Irkutsk
"SIP" -> 22, //Simferopol
"TFU" -> 22, //Chengdu
"SSA" -> 21, //Salvador
"BUF" -> 21, //Buffalo
"CTM" -> 21, //Chetumal
"HTI" -> 21, //Hamilton Island Resort
"MID" -> 21, //Mérida
"IXB" -> 21, //Bagdogra Darjeeling
"NCE" -> 20, //Nice
"BER" -> 20, //Berlin
"SAI" -> 20, //Siem Reap
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
"RUH" -> 20,
"FNC" -> 19, //Funchal
"AMM" -> 19, //Amman
"LIH" -> 19, //Lihue
"AMD" -> 19, //Ahmedabad
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
"PEN" -> 15, //Penang
"IAD" -> 15, //Washington
"LCA" -> 15, //Larnarca
"JNB" -> 15, //Johannesburg
"PLS" -> 15, //Providenciales Turks and Caicos
"ASP" -> 15, //Alice Springs
"BLQ" -> 15, //Bologna
"SNA" -> 15, //Santa Ana
"YYT" -> 15, //St John
"XMN" -> 15, //Xiamen
"ISG" -> 15, //Ishigaki JP
"JAC" -> 14, //Jackson
"OTP" -> 14, //Bucharest
"YHZ" -> 14, //Halifax
"CWB" -> 14, //Curitiba
"LPQ" -> 12, //Luang Phabang
"FAT" -> 12, //Yosemite National Park USA
"HDS" -> 12, //Kruger National Park South Africa
"NGO" -> 12, //Tokoname
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
"ZIA" -> 10, //Moscow
"HRB" -> 10, //Harbin
"CSX" -> 10, //Changsha
"OMA" -> 10, //Pico de Orizaba National Park Mexico
"ISG" -> 10, //Ishigaki
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
"LDH" -> 5,
"CMF" -> 5, //Chambéry
"CPX" -> 5, //Culebra PR
"VQS" -> 5 //Vieques PR
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
"SVO" -> 29, //Moscow
"EDI" -> 29, //Edinburgh
"IST" -> 28, //Istanbul
"PUS" -> 28, //Busan
"DEL" -> 28, //New Delhi
"BOG" -> 26, //Bogota
"EZE" -> 26, //Buenos Aires
"CPT" -> 25, //Cape Town
"FCO" -> 25, //Rome
"MXP" -> 25, //Milan
"ORY" -> 25, //Paris
"PKX" -> 25, //Beijing
"YYC" -> 25, //Calgary
"BAH" -> 25, //Bahrain
"ATL" -> 24, //Atlanta
"CLT" -> 24, //Charlotte
"GMP" -> 24, //Seoul
"SHA" -> 24, //Shanghai
"LOS" -> 24, //Lagos
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
"NBO" -> 18, //Nairobi
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
"PTY" -> 9, //Panama City
"BWI" -> 9, //Baltimore
"BGO" -> 9, //Bergen
"MLA" -> 9, //Malta
"ATH" -> 9, //Athens
"TFU" -> 9, //Chengdu
"MDE" -> 9, //Medellin
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
"BLL" -> 8, //Aarhus
"LCA" -> 8, //Nicosia
"NKG" -> 8, //Nanjing
"CTU" -> 8, //Chengdu
"BDA" -> 8, //Bermuda
"ADL" -> 8, //Adelaide
"PDX" -> 7, //Portland
"PIT" -> 7, //Pittsburgh
"NQZ" -> 7, //Nur-Sultan
"DLC" -> 7, //Dalian
"GYD" -> 7, //Baku
"PNQ" -> 6, //Pune
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
"BEG" -> 5, //Belgrade
"DUR" -> 5 //Durban
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
